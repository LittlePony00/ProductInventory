package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.request.CreateCategoryRequest
import com.android.rut.miit.productinventory.application.dto.request.UpdateCategoryRequest
import com.android.rut.miit.productinventory.application.dto.response.CategoryResponse
import com.android.rut.miit.productinventory.application.mapper.toResponse
import com.android.rut.miit.productinventory.domain.port.inbound.ICategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/households/{householdId}/categories")
class CategoryController(
    private val categoryService: ICategoryService
) {
    @GetMapping
    fun getCategories(
        @PathVariable householdId: UUID,
        @RequestParam(defaultValue = "false") includeArchived: Boolean
    ): List<CategoryResponse> =
        categoryService.getCategories(currentUserId(), householdId, includeArchived).map { it.toResponse() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(
        @PathVariable householdId: UUID,
        @Valid @RequestBody request: CreateCategoryRequest
    ): CategoryResponse =
        categoryService.createCategory(currentUserId(), householdId, request.name).toResponse()

    @PutMapping("/{categoryId}")
    fun updateCategory(
        @PathVariable householdId: UUID,
        @PathVariable categoryId: UUID,
        @Valid @RequestBody request: UpdateCategoryRequest
    ): CategoryResponse =
        categoryService.updateCategory(currentUserId(), householdId, categoryId, request.name).toResponse()

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun archiveCategory(
        @PathVariable householdId: UUID,
        @PathVariable categoryId: UUID
    ) {
        categoryService.archiveCategory(currentUserId(), householdId, categoryId)
    }
}
