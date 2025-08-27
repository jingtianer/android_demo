package com.jingtian.demoapp.main

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.SystemClock
import android.text.TextPaint
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.ColorInt
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jingtian.demoapp.main.ReflectToStringUtil.reflectToString
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

lateinit var app: DemoApplication

val Float.dp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        app.resources.displayMetrics
    )

val Float.px2Dp: Float
    get() {
        val scale = app.resources.displayMetrics.density
        // 四舍五入避免小数导致的精度问题
        return (this / scale + 0.5f);
    }

class ReflectField constructor(private val obj: Any, private val field: Field) {
    init {
        field.isAccessible = true
    }

    fun <T> get(): T? {
        return field.get(obj) as? T
    }

    override fun toString(): String {
        return "$field = ${get<Any>().reflectToString()}"
    }
}

class ReflectMethod constructor(var obj: Any, private val method: Method) {
    init {
        method.isAccessible = true
    }

    fun <R> call(args: Array<out Any>): R? {
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

    fun <R> call(args: Array<out Any>): R? {
        return constructor.newInstance(*args) as? R
    }

    override fun toString(): String {
        return constructor.toString()
    }
}

abstract class Reflect {
    companion object {
        fun create(clazz: Class<*>): ReflectClass {
            return ReflectClass(clazz)
        }

        fun create(obj: Any): Reflect {
            return ReflectObject(obj)
        }

        fun create(clazzName: String): ReflectClass {
            return ReflectClass(Class.forName(clazzName))
        }
    }

    abstract fun field(name: String): ReflectField
    abstract fun method(name: String, args: Array<Class<out Any>>): ReflectMethod

    fun <R> call(name: String, args: Array<out Any>): R? {
        return method(name, args.map { it.javaClass }.toTypedArray()).call(args)
    }
}

class ReflectObject : Reflect {
    private val clazz: Class<*>
    private val obj: Any

    constructor(obj: Any) {
        this.clazz = obj.javaClass
        this.obj = obj
    }

    override fun field(name: String): ReflectField {
        try {
            return ReflectField(obj, clazz.getField(name))
        } catch (ignore: NoSuchFieldException) {

        } catch (e: SecurityException) {
            throw SecurityException(name, e)
        }
        var clazz: Class<*>? = this.clazz
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(name)
                return ReflectField(obj, field)
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        throw NoSuchFieldException(name)
    }

    override fun method(name: String, args: Array<Class<out Any>>): ReflectMethod {
        try {
            return ReflectMethod(obj, clazz.getMethod(name, *args))
        } catch (ignore: NoSuchMethodException) {

        } catch (e: SecurityException) {
            throw SecurityException(name, e)
        }
        var clazz: Class<*>? = this.clazz
        while (clazz != null) {
            try {
                return ReflectMethod(obj, clazz.getDeclaredMethod(name, *args))
            } catch (e: NoSuchMethodException) {
                clazz = clazz.superclass
            } catch (e: SecurityException) {
                throw SecurityException(name, e)
            }
        }
        throw NoSuchMethodException("name=$name, args=${args.contentDeepToString()}")
    }

    override fun toString(): String {
        val sb = StringBuilder()
        var clazz: Class<*>? = clazz
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
            } catch (e: NoSuchFieldException) {
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
            } catch (e: NoSuchMethodException) {
                clazz = clazz.superclass
            } catch (e: SecurityException) {
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
        var clazz: Class<*>? = clazz
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
        return when (this) {
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
            when (item) {
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

object ScreenUtils {
    val Context.screenWidth: Int
        get() {
            val outMetrics = DisplayMetrics()
            getSystemService(WindowManager::class.java).defaultDisplay.getMetrics(outMetrics)
            return outMetrics.widthPixels
        }


    val Context.screenHeight: Int
        get() {
            val outMetrics = DisplayMetrics()
            getSystemService(WindowManager::class.java).defaultDisplay.getMetrics(outMetrics)
            return outMetrics.heightPixels
        }
}

object RxEvents {

    class DoubleClickListener(private val view: View, private val interval: Long) :
        ObservableOnSubscribe<Unit> {
        private var lastTime = -1L
        override fun subscribe(emitter: ObservableEmitter<Unit>) {
            view.setOnClickListener {
                val currentTime = SystemClock.elapsedRealtime()
                if (lastTime == -1L) {
                    lastTime = currentTime
                } else if ((currentTime - lastTime) < interval) {
                    emitter.onNext(Unit)
                    lastTime = -1L
                } else {
                    lastTime = currentTime
                }
            }
        }
    }

    fun View.setDoubleClickListener(interval: Long, onClick: () -> Unit) {
        val ignore = Observable.create(DoubleClickListener(this, interval))
            .subscribe {
                onClick()
            }
    }
}

object TextUtils {
    fun TextPaint.measure(text: String, outArray: FloatArray) {
        val textWidth = measureText(text)
        val textHeight = (fontMetrics.bottom - fontMetrics.top)
        if (outArray.size >= 2) {
            outArray[0] = textWidth
            outArray[1] = textHeight
        }
    }
}

object ColorUtils {
    @get:ColorInt
    val Int.revers: Int
        get() {
            return Color.argb(
                Color.alpha(this),
                255 - Color.red(this),
                255 - Color.green(this),
                255 - Color.blue(this),
            )
        }
}

class MutableLazy<T>(private val initializer: () -> T) : ReadWriteProperty<Any, T>, Lazy<T> {
    private var _value: T? = null

    override val value: T
        get() {
            var _value = _value
            return if (_value == null) {
                _value = initializer()
                this._value = _value
                _value
            } else {
                _value
            }
        }

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return value
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this._value = value
    }

    override fun isInitialized(): Boolean {
        return _value != null
    }

}

fun MotionEvent.insideOfView(view: View): Boolean {
    val location = IntArray(2)
    view.getLocationOnScreen(location)
    val x = location[0]
    val y = location[1]
    val w = view.width
    val h = view.height
    return this.rawX >= x && this.rawX <= x + w && this.rawY >= y && this.rawY <= y + h
}

object StorageUtil {
    open class StorageVariable<V, T>(
        private val sp: SharedPreferences,
        private val key: String,
        defaultValue: T,
        getValue: SharedPreferences.(String, T) -> T,
        private val putValue: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
    ) : ReadWriteProperty<V, T> {
        private var value = sp.getValue(key, defaultValue)

        override fun getValue(thisRef: V, property: KProperty<*>): T {
            return value
        }

        override fun setValue(thisRef: V, property: KProperty<*>, value: T) {
            this.value = value
            sp.edit().putValue(key, value).apply()
        }
    }

    class StorageBoolean<T>(
        sp: SharedPreferences,
        key: String,
        defaultValue: Boolean
    ) : StorageVariable<T, Boolean>(
        sp, key, defaultValue,
        SharedPreferences::getBoolean,
        SharedPreferences.Editor::putBoolean
    )

    class StorageLong<T>(
        sp: SharedPreferences,
        key: String,
        defaultValue: Long
    ) : StorageVariable<T, Long>(
        sp, key, defaultValue,
        SharedPreferences::getLong,
        SharedPreferences.Editor::putLong
    )

    class SynchronizedProperty<T, V>(
        private val property: ReadWriteProperty<T, V>
    ) : ReadWriteProperty<T, V> {

        @Synchronized
        override fun getValue(thisRef: T, property: KProperty<*>): V {
            return this.property.getValue(thisRef, property)
        }

        @Synchronized
        override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
            this.property.setValue(thisRef, property, value)
        }

    }

    class StorageString<T>(
        sp: SharedPreferences,
        key: String,
        defaultValue: String
    ) : StorageVariable<T, String>(
        sp, key, defaultValue,
        { k, v -> getString(k, v) ?: v },
        SharedPreferences.Editor::putString
    )

    class StorageJson<T, V>(
        sp: SharedPreferences,
        key: String,
        defaultValue: V,
        private val gson: Gson,
        typeToken: TypeToken<V>
    ) : ReadWriteProperty<T, V> {
        private var json by StorageString(sp, key, gson.toJson(defaultValue))
        private var value = gson.fromJson(json, typeToken.type) as V
        override fun getValue(thisRef: T, property: KProperty<*>): V {
            return value
        }

        override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
            this.value = value
            json = gson.toJson(value)
        }

    }
}