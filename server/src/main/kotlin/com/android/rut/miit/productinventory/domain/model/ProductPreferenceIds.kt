package com.android.rut.miit.productinventory.domain.model

import java.util.UUID

fun Product.preferenceCategoryId(): UUID =
    categoryId ?: SystemCategoryCatalog.idFor(category)
