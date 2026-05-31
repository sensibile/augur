package me.sensibile.augur.rule.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/rule-sets")
class RuleSetValidationController(
    private val validationService: RuleSetValidatorPort,
) {
    @PostMapping("/validate", consumes = ["application/json"], produces = ["application/json"])
    fun validate(
        @RequestBody body: String,
    ): ResponseEntity<RuleSetValidationResponse> =
        when (val result = validationService.validateCanonicalJson(body)) {
            is RuleSetValidationResult.Valid -> {
                ResponseEntity.ok(
                    RuleSetValidationResponse(
                        valid = true,
                        summary = result.summary,
                    ),
                )
            }

            is RuleSetValidationResult.Invalid -> {
                ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(
                        RuleSetValidationResponse(
                            valid = false,
                            error = result.error,
                        ),
                    )
            }
        }
}
