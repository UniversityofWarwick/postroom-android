package uk.ac.warwick.postroom

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import uk.ac.warwick.postroom.services.CustomTabsService
import uk.ac.warwick.postroom.services.CustomTabsServiceImpl
import uk.ac.warwick.postroom.services.SscPersistenceService
import uk.ac.warwick.postroom.services.SscPersistenceServiceImpl

@Module
@InstallIn(ActivityComponent::class)
abstract class PostroomActivityModule {
    @Binds
    abstract fun bindCustomTabService(
        customTabsServiceImpl: CustomTabsServiceImpl
    ): CustomTabsService

    @Binds
    abstract fun bindSscPersistenceService(
        sscPersistenceServiceImpl: SscPersistenceServiceImpl
    ): SscPersistenceService
}
