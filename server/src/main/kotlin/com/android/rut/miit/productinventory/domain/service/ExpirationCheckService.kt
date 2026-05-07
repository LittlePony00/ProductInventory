package com.android.rut.miit.productinventory.domain.service

import com.android.rut.miit.productinventory.domain.model.ExpirationStatus
import com.android.rut.miit.productinventory.domain.model.Product

class ExpirationCheckService {

    fun filterExpiring(products: List<Product>): List<Product> =
        products.filter { product ->
            product.expirationDate?.status == ExpirationStatus.EXPIRING_SOON
        }

    fun filterExpired(products: List<Product>): List<Product> =
        products.filter { product ->
            product.expirationDate?.status == ExpirationStatus.EXPIRED
        }

    fun sortByExpirationPriority(products: List<Product>): List<Product> =
        products.sortedWith(
            compareBy<Product> {
                when (it.expirationDate?.status) {
                    ExpirationStatus.EXPIRED -> 0
                    ExpirationStatus.EXPIRING_SOON -> 1
                    ExpirationStatus.FRESH -> 2
                    ExpirationStatus.UNKNOWN, null -> 3
                }
            }.thenBy { it.expirationDate?.date }
        )
}
