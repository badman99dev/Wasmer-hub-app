package com.movie.app.best.ui.screens.profile

import com.movie.app.best.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileFirebaseViewModel @Inject constructor(
    val firebaseRepository: FirebaseRepository
) : androidx.lifecycle.ViewModel()
