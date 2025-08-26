package com.jingtian.demoapp.main.rank.model

import android.net.Uri
import com.google.gson.InstanceCreator
import java.lang.reflect.Type

data class RankItemImage(var id: Long = -1, var image: Uri = Uri.EMPTY)

data class ModelRankItem(
    var name: String = "",
    var score: Float = 0f,
    var desc: String = "",
    var comments: List<ModelItemComment> = mutableListOf(),
    var image: RankItemImage = RankItemImage(),
) {
    companion object {
        fun ModelRankItem.isValid(): Boolean {
            return name.isNotEmpty()
        }
    }
}