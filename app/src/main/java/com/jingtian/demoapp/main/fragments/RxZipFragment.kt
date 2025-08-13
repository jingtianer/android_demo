package com.jingtian.demoapp.main.fragments

import android.os.Bundle
import android.view.View
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.Arrays

class RxZipFragment : LogFragment("RxJava\nTest Zip") {
    private var disposable: Disposable? = null

    override fun onResume() {
        super.onResume()
        disposable = Observable.create { emitter->
            try {
                val task1 = task(2000)
                val task2 = task(3000)
                val task3 = task(5000)
                val mergedTask = Observable.zip(task1, task2, task3) { a, b, c->
                    longArrayOf(a, b, c)
                }
                asyncAddLog("[${Thread.currentThread().name}] all sub task create Complete")
                emitter.onNext(mergedTask)
                emitter.onComplete()
            } catch (e: Exception) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
            }
        }.flatMap { it }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({it : LongArray->
                asyncAddLog("subscribe->${it.contentToString()}")
            }, {
                asyncAddLog("onError-$it")
            }, {
                asyncAddLog("Complete")
            })
        asyncAddLog("task create Complete")
    }

    private fun task(time: Long): Observable<Long> {
        try {
            Thread.sleep(time / 10)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        asyncAddLog("${Thread.currentThread().name} sub task created-$time")
        return Observable.create { emitter ->
            try {
                Thread.sleep(time)
            } catch (e: InterruptedException) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
            }
            asyncAddLog("${Thread.currentThread().name} emitted-$time")
            emitter.onNext(time)
            emitter.onComplete()
        }.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
    }

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
        disposable = null
    }
}