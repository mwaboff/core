# Feature Cost Tags in Card/Adversary Expansion

## Context

When parent entities (cards, adversaries) expand their features via `?expand=features`, the nested `FeatureResponse` objects are built inline and **do not include cost tag data**. Cost tags should be included to match the behavior of the standalone Feature endpoint.

## Approach

Update the feature expansion block in all 5 services that expand features to:
1. **Always include `costTagIds`** — list of cost tag IDs in every nested feature
2. **Include full `costTags`** when `?expand=costTags` is also present — full `CardCostTagResponse` objects with `id`, `label`, `category`, timestamps

This mirrors the existing pattern in `FeatureService.toResponse()` (lines 284-303).

## File Changes

### Services (5 files)

Each service's `toResponse()` method has an identical feature expansion block that needs the same modification:

| Service | File | Feature expansion lines |
|---------|------|------------------------|
| AncestryCardService | `src/main/java/.../service/dh/AncestryCardService.java` | 329-342 |
| CommunityCardService | `src/main/java/.../service/dh/CommunityCardService.java` | 329-342 |
| DomainCardService | `src/main/java/.../service/dh/DomainCardService.java` | 366-379 |
| SubclassCardService | `src/main/java/.../service/dh/SubclassCardService.java` | 402-415 |
| AdversaryService | `src/main/java/.../service/dh/AdversaryService.java` | 673-686 |

**Change**: Replace the inline FeatureResponse builder in each service to add cost tag IDs and conditional cost tag expansion after building the base fields.

Before (current):
```java
if (expand.contains("features") && card.getFeatures() != null) {
    builder.features(card.getFeatures().stream()
            .map(feature -> FeatureResponse.builder()
                    .id(feature.getId())
                    .name(feature.getName())
                    .description(feature.getDescription())
                    .featureType(feature.getFeatureType())
                    .expansionId(feature.getExpansion().getId())
                    .createdAt(feature.getCreatedAt())
                    .lastModifiedAt(feature.getLastModifiedAt())
                    .deletedAt(feature.getDeletedAt())
                    .build())
            .collect(Collectors.toList()));
}
```

After:
```java
if (expand.contains("features") && card.getFeatures() != null) {
    builder.features(card.getFeatures().stream()
            .map(feature -> {
                FeatureResponse.FeatureResponseBuilder featureBuilder = FeatureResponse.builder()
                        .id(feature.getId())
                        .name(feature.getName())
                        .description(feature.getDescription())
                        .featureType(feature.getFeatureType())
                        .expansionId(feature.getExpansion().getId())
                        .createdAt(feature.getCreatedAt())
                        .lastModifiedAt(feature.getLastModifiedAt())
                        .deletedAt(feature.getDeletedAt());

                // Always include cost tag IDs
                if (feature.getCostTags() != null) {
                    featureBuilder.costTagIds(feature.getCostTags().stream()
                            .map(CardCostTag::getId)
                            .collect(Collectors.toList()));
                }

                // Expand cost tags if requested
                if (expand.contains("costTags") && feature.getCostTags() != null) {
                    featureBuilder.costTags(feature.getCostTags().stream()
                            .map(tag -> CardCostTagResponse.builder()
                                    .id(tag.getId())
                                    .label(tag.getLabel())
                                    .category(tag.getCategory())
                                    .createdAt(tag.getCreatedAt())
                                    .lastModifiedAt(tag.getLastModifiedAt())
                                    .deletedAt(tag.getDeletedAt())
                                    .build())
                            .collect(Collectors.toList()));
                }

                return featureBuilder.build();
            })
            .collect(Collectors.toList()));
}
```

Note: AdversaryService uses `.collect(Collectors.toSet())` instead of `.toList()`.

### Tests (5 files)

| Test | File |
|------|------|
| AncestryCardServiceTest | `src/test/java/.../service/dh/AncestryCardServiceTest.java` |
| CommunityCardServiceTest | `src/test/java/.../service/dh/CommunityCardServiceTest.java` |
| DomainCardServiceTest | `src/test/java/.../service/dh/DomainCardServiceTest.java` |
| SubclassCardServiceTest | `src/test/java/.../service/dh/SubclassCardServiceTest.java` |
| AdversaryServiceTest | `src/test/java/.../service/dh/AdversaryServiceTest.java` |

For each test, update existing feature expansion tests to:
1. Add cost tags to mock Feature entities
2. Assert `costTagIds` is always present in expanded features
3. Add test for `?expand=features,costTags` verifying full cost tag objects are included
4. Add test for `?expand=features` (without costTags) verifying `costTags` is null but `costTagIds` is present

### Imports

Each service file may need these additional imports:
- `com.aboff.core.model.entity.dh.CardCostTag`
- `com.aboff.core.model.dto.dh.response.CardCostTagResponse`

## Testing Strategy

- Run existing tests first to establish baseline
- Implement changes in services
- Update/add test cases
- Run full test suite to verify all pass
