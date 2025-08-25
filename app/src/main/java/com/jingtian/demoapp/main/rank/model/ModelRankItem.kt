package com.jingtian.demoapp.main.rank.model

import android.net.Uri

data class ModelRankItem(
    var name: String,
    var score: Float,
    var desc: String,
    var comments: List<ModelItemComment>,
    var image: Uri = Uri.EMPTY,
) {
    companion object {
        fun ModelRankItem.isValid(): Boolean {
            return name.isNotEmpty()
        }
    }
}