package uk.ac.warwick.postroom

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import uk.ac.warwick.postroom.services.*

@Module
@InstallIn(ActivityComponent::class)
abstract class PostroomActivityModule {
    @Binds
    abstract fun bindProvidesBaseUrl(
        providesBaseUrlImpl: ProvidesBaseUrlImpl
    ): ProvidesBaseUrl

    @Binds
    abstract fun bindCustomTabService(
        customTabsServiceImpl: CustomTabsServiceImpl
    ): CustomTabsService

    @Binds
    abstract fun bindRecipientDataService(
        cachedRecipientDataServiceImpl: RecipientDataServiceImpl
    ): RecipientDataService

    @Binds
    abstract fun bindCourierMatchPatternService(
        courierMatchService: CourierMatchServiceImpl
    ): CourierMatchService

    @Binds
    abstract fun bindItemService(
        itemService: ItemServiceImpl
    ): ItemService

    @Binds
    abstract fun bindSscPersistenceService(
        sscPersistenceServiceImpl: SscPersistenceServiceImpl
    ): SscPersistenceService
}
