package com.fuwanime.util

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.fuwanime.AppContainer
import com.fuwanime.FuwaApp

private fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as FuwaApp).container

/** `val vm: HomeViewModel = fuwaViewModel { c, _ -> HomeViewModel(c.animeRepository) }` */
@Composable
inline fun <reified VM : ViewModel> fuwaViewModel(
    key: String? = null,
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): VM = viewModel(
    key = key,
    factory = viewModelFactory {
        initializer { create(containerOf(this), createSavedStateHandle()) }
    },
)

@PublishedApi
internal fun containerOf(extras: CreationExtras): AppContainer = extras.container()
