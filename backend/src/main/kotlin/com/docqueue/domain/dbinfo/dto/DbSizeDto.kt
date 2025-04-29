package com.docqueue.domain.dbinfo.dto

data class DbSizeDto(
    val dbName: String,
    val dbSizeMb: Double,
    val dataSizeMb: Double,
    val indexSizeMb: Double
) {
    // FP 스타일로 DTO 변환 함수 제공
    companion object {
        fun fromRaw(raw: Array<Any>): DbSizeDto = DbSizeDto(
            dbName = raw[0].toString(),
            dbSizeMb = (raw[1] as Number).toDouble(),
            dataSizeMb = (raw[2] as Number).toDouble(),
            indexSizeMb = (raw[3] as Number).toDouble()
        )
    }

    // 데이터 변환 예시 (고차 함수 활용)
    fun <R> transform(transformer: (DbSizeDto) -> R): R = transformer(this)
}