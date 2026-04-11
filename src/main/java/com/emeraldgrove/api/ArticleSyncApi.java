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
import org.springframework.web.bind.annotation.PathVariable;
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

    @Operation(
        summary = "Delete note",
        description = "Deletes a note from an article by the article externalId and noteId."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Note deleted successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Article or note not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    ResponseEntity<Void> deleteNote(
        @PathVariable String externalId,
        @PathVariable String noteId
    );

    @Operation(
        summary = "Delete article",
        description = "Deletes an article and all of its notes by externalId."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Article deleted successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Article not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    ResponseEntity<Void> deleteArticle(@PathVariable String externalId);
}
