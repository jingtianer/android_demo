package com.jingtian.demoapp.main.widget.appbar

interface IAppBarAdapter {
    /**
     * @return
     * true: scrollable
     * false: notScrollable
     * */
    fun getScrollMode(position: Int): Boolean
}