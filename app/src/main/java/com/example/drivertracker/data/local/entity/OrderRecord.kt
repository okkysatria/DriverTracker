package com.example.drivertracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_records")
data class OrderRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tanggal: String = "",
    val hari: String = "",
    val jamMulai: Long = 0L,
    val jamPickup: Long = 0L,
    val jamSelesai: Long = 0L,
    val jenisOrder: String = "Penumpang",
    val pendapatanBersih: Double = 0.0,
    val pendapatanKotor: Double = 0.0,
    val durasi: Long = 0L,
    val jarakTempuh: Double = 0.0,
    val jarakKePickup: Double = 0.0,
    val jarakKeTujuan: Double = 0.0,
    val latitudeAwal: Double = 0.0,
    val longitudeAwal: Double = 0.0,
    val alamatAwal: String = "",
    val latitudePickup: Double = 0.0,
    val longitudePickup: Double = 0.0,
    val alamatPickup: String = "",
    val latitudeAkhir: Double = 0.0,
    val longitudeAkhir: Double = 0.0,
    val alamatAkhir: String = "",
    val catatan: String = "",
    val trackGpsJson: String = "[]",
    val biayaBensin: Double = 0.0
)
