package com.emeraldgrove.api;

import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.dto.SyncArticleResponse;
import com.emeraldgrove.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Articles", description = "Article synchronization endpoints")
public interface ArticleSyncApi {

    @Operation(
        summary = "Synchronize article",
        description = "Creates or updates an article and returns the persisted server snapshot."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Article created successfully",
            content = @Content(schema = @Schema(implementation = SyncArticleResponse.class))
        ),
        @ApiResponse(
            responseCode = "200",
            description = "Article updated successfully",
            content = @Content(schema = @Schema(implementation = SyncArticleResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Article already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    ResponseEntity<SyncArticleResponse> syncArticle(@Valid @RequestBody SyncArticleRequest request);
}
