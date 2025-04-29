package com.docqueue.domain.dbinfo.repository

import kotlinx.coroutines.flow.Flow
import com.docqueue.domain.dbinfo.dto.DbSizeDto

interface DbStatusRepository {
    fun getDbSizeInfo(): Flow<DbSizeDto>
}