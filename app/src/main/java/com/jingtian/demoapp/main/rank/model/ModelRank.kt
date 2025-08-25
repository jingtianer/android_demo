package com.jingtian.demoapp.main.rank.model

data class ModelRank(
    val name: String,
    val list: List<ModelRankItem>
) {
    companion object {
        fun ModelRank.isValid(): Boolean {
            return name.isNotEmpty()
        }
    }
}