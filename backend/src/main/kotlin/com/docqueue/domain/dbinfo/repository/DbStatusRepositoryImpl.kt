package com.docqueue.domain.dbinfo.repository

import com.docqueue.domain.dbinfo.dto.DbSizeDto
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository

@Repository
class DbStatusRepositoryImpl(
    private val databaseClient: DatabaseClient
) : DbStatusRepository {
    private val query: String = """
SELECT 
    current_database() AS dbName,
    ROUND(SUM(pg_total_relation_size(c.oid)) / 1024.0 / 1024.0, 2) AS dbSizeMb,
    ROUND(SUM(pg_table_size(c.oid)) / 1024.0 / 1024.0, 2) AS dataSizeMb,
    ROUND(SUM(pg_indexes_size(c.oid)) / 1024.0 / 1024.0, 2) AS indexSizeMb
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname NOT IN ('pg_catalog', 'information_schema')
GROUP BY current_database();
""".trimIndent()

    // FP 스타일로 순수 함수와 Flow 사용
    override fun getDbSizeInfo(): Flow<DbSizeDto> =
        databaseClient.sql(query)
            .map { row, _ ->
                DbSizeDto(
                    dbName = row.get("dbName", String::class.java) ?: "",
                    dbSizeMb = row.get("dbSizeMb", Double::class.java) ?: 0.0,
                    dataSizeMb = row.get("dataSizeMb", Double::class.java) ?: 0.0,
                    indexSizeMb = row.get("indexSizeMb", Double::class.java) ?: 0.0
                )
            }
            .flow()
}