package com.example.myrecipeapp.di

import android.content.Context
import com.example.myrecipeapp.data.local.AppDatabase
import com.example.myrecipeapp.data.local.CachedRecipeDao
import com.example.myrecipeapp.data.local.FavoriteDao
import com.example.myrecipeapp.data.local.FeaturedCacheDao
import com.example.myrecipeapp.data.local.ShoppingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideShoppingDao(db: AppDatabase): ShoppingDao = db.shoppingDao()

    @Provides
    fun provideCachedRecipeDao(db: AppDatabase): CachedRecipeDao = db.cachedRecipeDao()

    @Provides
    fun provideFeaturedCacheDao(db: AppDatabase): FeaturedCacheDao = db.featuredCacheDao()
}
