package com.jingtian.demoapp.main.fragments

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class RxJavaFragment : LogFragment("RxJava-GroupBy-Throttle") {
    private var disposable: Disposable? = null
    override fun onResume() {
        super.onResume()
        val mapper: io.reactivex.functions.Function<Long, Int> = object: io.reactivex.functions.Function<Long, Int> {
            var i = 0
            override fun apply(t: Long): Int {
                return i++
            }

        }
        disposable = Observable.interval(1000, TimeUnit.MILLISECONDS).map(mapper)
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                addLog("create: $it")
                it
            }
            .observeOn(AndroidSchedulers.mainThread())
            .groupBy {
                it % 2
            }
            .flatMap {
                val key = it.key
                if (key != null && key == 0) {
                    it.throttleFirst(8000, TimeUnit.MILLISECONDS)
                } else {
                    it
                }
            }
            .subscribe { i: Int ->
                addLog("subscribe: $i")
            }
    }

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
        clearLog()
    }
}