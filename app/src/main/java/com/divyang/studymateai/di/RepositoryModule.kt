package com.divyang.studymateai.di

import com.divyang.studymateai.data.repository.AccountRepository
import com.divyang.studymateai.data.repository.AuthRepository
import com.divyang.studymateai.data.repository.ChapterRepository
import com.divyang.studymateai.data.repository.QuizHistoryRepository
import com.divyang.studymateai.data.repository.UserRepository
import com.divyang.studymateai.data.repository.impl.AccountRepositoryImpl
import com.divyang.studymateai.data.repository.impl.AuthRepositoryImpl
import com.divyang.studymateai.data.repository.impl.ChapterRepositoryImpl
import com.divyang.studymateai.data.repository.impl.QuizHistoryRepositoryImpl
import com.divyang.studymateai.data.repository.impl.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindChapterRepository(impl: ChapterRepositoryImpl): ChapterRepository

    @Binds
    @Singleton
    abstract fun bindQuizHistoryRepository(impl: QuizHistoryRepositoryImpl): QuizHistoryRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
}
