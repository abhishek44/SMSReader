package com.smsreader.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smsreader.app.di.AppContainer
import com.smsreader.app.ui.analytics.AnalyticsViewModel
import com.smsreader.app.ui.budget.BudgetViewModel
import com.smsreader.app.ui.review.ReviewViewModel

fun reviewViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer { ReviewViewModel(container.smsReviewRepository, container.financeRepository, container.dataBackupRepository) }
}

fun analyticsViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer { AnalyticsViewModel(container.financeRepository) }
}

fun budgetViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer { BudgetViewModel(container.financeRepository) }
}
