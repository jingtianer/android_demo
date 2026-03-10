package com.jingtian.composedemo.main.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import demoapp.composedemo.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import kotlin.math.max

@Composable
fun DesktopStarRateView(
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    initialScore: Float = 0f,
    onScoreChange: (Float) -> Unit = {}
) {
    val appThemeViewModel: AppThemeViewModel = viewModel(factory = AppThemeViewModel.viewModelFactory)
    val appTheme by remember { appThemeViewModel.currentAppTheme }
    val isSystemDark = isSystemInDarkTheme()
    val isNight = AppTheme.isDark(appTheme, isSystemDark)
    @Composable
    fun RowScope.StarView(score: Int, currentSore: Int, onClick: (Int)->Unit) {
        Image(
            painter = if (isNight) {
                if (currentSore >= score) {
                    painterResource(Res.drawable.star_high_lighted_night)
                } else {
                    painterResource(Res.drawable.star_night)
                }
            } else {
                if (currentSore >= score) {
                    painterResource(Res.drawable.star_high_lighted)
                } else {
                    painterResource(Res.drawable.star)
                }
            },
            contentDescription = "评分: $score",
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp).size(30.dp).weight(1f).clickable(enabled = enable) {
                if (enable) {
                    if (score == currentSore) {
                        onClick(max(score - 1, 0))
                    } else {
                        onClick(score)
                    }
                }
            }
        )
    }
    Row(modifier) {
        StarView(1, initialScore.toInt()) {score->
            onScoreChange(score.toFloat())
        }
        StarView(2, initialScore.toInt()) {score->
            onScoreChange(score.toFloat())
        }
        StarView(3, initialScore.toInt()) {score->
            onScoreChange(score.toFloat())
        }
        StarView(4, initialScore.toInt()) {score->
            onScoreChange(score.toFloat())
        }
        StarView(5, initialScore.toInt()) {score->
            onScoreChange(score.toFloat())
        }
    }
}
