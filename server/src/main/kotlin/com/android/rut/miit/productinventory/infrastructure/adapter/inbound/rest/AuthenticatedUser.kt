package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

fun currentUserId(): UUID =
    SecurityContextHolder.getContext().authentication.principal as UUID
