package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "barcode_products")
class BarcodeProductEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    var barcode: String = "",

    @Column(nullable = false)
    var name: String = "",

    @Column
    var category: String? = null,

    @Column(name = "image_url")
    var imageUrl: String? = null,

    @Column(name = "fetched_at", nullable = false)
    var fetchedAt: Instant = Instant.now()
)
