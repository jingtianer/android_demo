package com.jingtian.demoapp.main

import android.app.Application
import android.util.TypedValue
import com.jingtian.demoapp.main.ReflectToStringUtil.reflectToString
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

lateinit var app: Application

val Float.dp: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, app.resources.displayMetrics)


class ReflectField constructor(private val obj: Any, private val field: Field) {
    init {
        field.isAccessible = true
    }

    fun <T> get() : T? {
        return field.get(obj) as? T
    }

    override fun toString() : String {
        return "$field = ${get<Any>().reflectToString()}"
    }
}

class ReflectMethod constructor(private val obj: Any, private val method: Method) {
    init {
        method.isAccessible = true
    }

    fun <R> call(args : Array<out Any>): R? {
        return method.invoke(obj, *args) as? R
    }

    override fun toString(): String {
        return method.toString()
    }
}

class ReflectConstructor constructor(private val constructor: Constructor<*>) {
    init {
        constructor.isAccessible = true
    }

    fun <R> call(args : Array<out Any>): R? {
        return constructor.newInstance(*args) as? R
    }

    override fun toString(): String {
        return constructor.toString()
    }
}

abstract class Reflect {
    companion object {
        fun create(clazz: Class<*>) : ReflectClass {
            return ReflectClass(clazz)
        }

        fun create(obj: Any) : Reflect {
            return ReflectObject(obj)
        }

        fun create(clazzName: String) : ReflectClass {
            return ReflectClass(Class.forName(clazzName))
        }
    }
    abstract fun field(name: String): ReflectField
    abstract fun method(name: String, args: Array<Class<out Any>>): ReflectMethod

    fun <R> call(name: String, args: Array<out Any>): R? {
        return method(name, args.map { it.javaClass }.toTypedArray()).call(args)
    }
}

class ReflectObject : Reflect{
    private val clazz: Class<*>
    private val obj: Any

    constructor(obj: Any) {
        this.clazz = obj.javaClass
        this.obj = obj
    }

    override fun field(name: String): ReflectField {
        try {
            return ReflectField(obj, clazz.getField(name))
        } catch (ignore : NoSuchMethodException) {

        } catch (e : SecurityException) {
            throw SecurityException(name, e)
        }
        var clazz: Class<*>? = this.clazz
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(name)
                return ReflectField(obj, field)
            } catch (e : NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        throw NoSuchFieldException(name)
    }

    override fun method(name: String, args: Array<Class<out Any>>): ReflectMethod {
        try {
            return ReflectMethod(obj, clazz.getMethod(name, *args))
        } catch (ignore : NoSuchMethodException) {

        } catch (e : SecurityException) {
            throw SecurityException(name, e)
        }
        var clazz: Class<*>? = this.clazz
        while (clazz != null) {
            try {
                return ReflectMethod(obj, clazz.getDeclaredMethod(name, *args))
            } catch (e : NoSuchMethodException) {
                clazz = clazz.superclass
            } catch (e : SecurityException) {
                throw SecurityException(name, e)
            }
        }
        throw NoSuchMethodException("name=$name, args=${args.contentDeepToString()}")
    }

    override fun toString(): String {
        val sb = StringBuilder()
        var clazz: Class<*>?= clazz
        while (clazz != null) {
            sb.append("class = " + clazz.name + "\n")
            sb.append("Fields:\n")
            for (field in clazz.declaredFields) {
                if (Modifier.isStatic(field.modifiers)) {
                    sb.append(ReflectField(clazz, field).toString() + "\n")
                } else {
                    sb.append(ReflectField(obj, field).toString() + "\n")
                }
            }
            sb.append("Methods:\n")
            for (method in clazz.declaredMethods) {
                if (Modifier.isStatic(method.modifiers)) {
                    sb.append(ReflectMethod(clazz, method).toString() + "\n")
                } else {
                    sb.append(ReflectMethod(obj, method).toString() + "\n")
                }
            }
            clazz = clazz.superclass
        }
        return sb.toString()
    }
}

class ReflectClass : Reflect {
    private val clazz: Class<*>
    private val obj: Any

    constructor(clazz: Class<*>) {
        this.clazz = clazz
        this.obj = clazz
    }

    override fun field(name: String): ReflectField {
        var clazz: Class<*>? = this.clazz
        while (clazz != null) {
            try {
                return ReflectField(clazz, clazz.getDeclaredField(name))
            } catch (e : NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        throw NoSuchFieldException(name)
    }

    override fun method(name: String, args: Array<Class<out Any>>): ReflectMethod {
        var clazz: Class<*>? = this.clazz
        while (clazz != null) {
            try {
                return ReflectMethod(clazz, clazz.getDeclaredMethod(name, *args))
            } catch (e : NoSuchMethodException) {
                clazz = clazz.superclass
            } catch (e : SecurityException) {
                throw SecurityException(name, e)
            }
        }
        throw NoSuchMethodException("name=$name, args=${args.contentDeepToString()}")
    }

    fun constructor(args: Array<Class<out Any>>): ReflectConstructor {
        return ReflectConstructor(clazz.getDeclaredConstructor(*args))
    }

    fun <R> newInstance(args: Array<out Any>): R? {
        return constructor(args.map { it.javaClass }.toTypedArray()).call(args)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        var clazz: Class<*>?= clazz
        while (clazz != null) {
            sb.append("class = " + clazz.name + "\n")
            sb.append("Fields:\n")
            for (field in clazz.declaredFields) {
                if (Modifier.isStatic(field.modifiers)) {
                    sb.append(ReflectField(clazz, field).toString() + "\n")
                }
            }
            sb.append("Methods:\n")
            for (method in clazz.declaredMethods) {
                if (Modifier.isStatic(method.modifiers)) {
                    sb.append(ReflectMethod(clazz, method).toString() + "\n")
                }
            }
            clazz = clazz.superclass
        }
        return sb.toString()
    }
}

fun <T> Field.getStaticValue(cl: Class<out Any>, clazz: Class<T>): T? {
    if (type == clazz && Modifier.isStatic(modifiers)) {
        return get(cl) as? T
    }
    return null
}

fun <T> Field.getStaticValueString(cl: Class<out Any>, clazz: Class<T>): String? {
    return getStaticValue(cl, clazz).reflectToString()
}

object ReflectToStringUtil {
    fun Any?.reflectToString(): String {
        return when(this) {
            is Array<*> -> {
                reflectToString()
            }
            is String -> {
                reflectToString()
            }
            is Collection<*> -> {
                this.toTypedArray().reflectToString()
            }
            is Long -> {
                return "${this}L"
            }
            is Float -> {
                return "${this}f"
            }
            is Char -> {
                return "'$this'"
            }
            null -> {
                "null"
            }
            else -> {
                toString()
            }
        }
    }

    fun String.reflectToString(): String {
        return "\"$this\""
    }

    fun <T> Array<T>.reflectToString(): String {
        val sb = StringBuilder("[")
        for (item in this) {
            when(item) {
                is Array<*> -> {
                    sb.append(item.reflectToString())
                }
                is String -> {
                    sb.append(item.reflectToString())
                }
                else -> {
                    sb.append(item)
                }
            }
            sb.append(",")
        }
        sb[sb.length - 1] = ']'
        return sb.toString()
    }
}