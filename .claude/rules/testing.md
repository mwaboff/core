# Testing Requirements

## Test Coverage
1. **Minimum Requirement:** 80% Coverage
2. **All new or modified logic must have comprehensive tests** - any code written to service classes, model classes, controller classes, or other components must include tests that provide near 100% test coverage.
3. **Skip Coverage:** Lombok annotations, simple getters/setters, trivial configs

## Test Types (ALL required):
1. **Unit Tests** - Individual functions, utilities, components
2. **Integration Tests** - API endpoints, database operations

## Test Rules
1**REQUIREMENT:** DO create new or update existing tests for all new or modified code. Aim for 100% logic coverage.
2**REQUIREMENT:** DO run tests to validate all changes made when coding. All tests must pass (green). Correct code and re-run tests until all pass. 

DO name tests consistently - `{ClassName}Test` for unit tests, `{ClassName}IntegrationTest` for integration tests

## Edge Cases You MUST Test
- Null/Undefined: What if input is null?
- Empty: What if array/string is empty?
- Invalid Types: What if wrong type passed?
- Boundaries: Min/max values
- Errors: Network failures, database errors
- Race Conditions: Concurrent operations
- Special Characters: Unicode, emojis, SQL characters

## Best Practices

1. **One Assert Per Test** - Focus on single behavior
2. **Descriptive Test Names** - Explain what's tested
3. **Arrange-Act-Assert** - Clear test structure
4. **Mock External Dependencies** - Isolate unit tests
5. **Test Edge Cases** - Null, undefined, empty, large
6. **Test Error Paths** - Not just happy paths
7. **Keep Tests Fast** - Unit tests < 50ms each
8. **Clean Up After Tests** - No side effects
9. **Review Coverage Reports** - Identify gaps

## Success Metrics

- Near 100% code coverage achieved
- All tests passing (green)
- No skipped or disabled tests
- Fast test execution (< 50s for unit tests)
- Tests catch bugs before production

---

**Remember**: Tests are not optional. They are the safety net that enables confident refactoring, rapid development, and production reliability.