package com.jingtian.composedemo.main.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.main.labels.LabelView
import com.jingtian.composedemo.main.widget.ScoreStar
import com.jingtian.composedemo.viewmodels.appThemeViewModel

@Composable
fun FileExtraInfoView(albumItemRelation: AlbumItemRelation, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val padding = 4.dp
        Text(
            albumItemRelation.albumItem.itemName,
            modifier = Modifier.fillMaxWidth(),
            style = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight(700)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!albumItemRelation.albumItem.desc.isNullOrBlank()) {
            Text(
                albumItemRelation.albumItem.desc,
                modifier = Modifier.fillMaxWidth(),
                style = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight(400)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val itemScore = albumItemRelation.albumItem.score
        ScoreStar(itemScore, padding)
        val itemLabel = albumItemRelation.labelInfos
        if (itemLabel.isNotEmpty()) {
            LazyRow(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp, start = padding, end = padding)
            ) {
                items(itemLabel.size, key = { index: Int ->
                    itemLabel[index].label
                }) { index: Int ->
                    LabelView(itemLabel[index].label)
                }
            }
        }
    }
}