# Java Coding Standards

Aim for consistency with the below instructions and with the rest of the project. When in doubt, follow the [Google Java Coding Style Guide](https://google.github.io/styleguide/javaguide.html).

## Java Coding Expectations

### Logging
1. Use SLF4J for logging.
2. Add logs to allow for easy troubleshooting at important steps in logic.
3. Include helpful details in a single log when appropriate, such as variable values.
4. DO **NOT** log sensitive details. If in doubt, ask the user.
5. Avoid duplicate logs, it is better to have a single helpful message than multiple repetitive ones.

### Documentation
1. Add detailed javadocs to all methods and classes. Use standard Javadocs (`/** ... */`) with `@param`, `@return`, and `@throws` tags where applicable.
2. Ensure all public classes and methods have documentation explaining their purpose and behavior.
3. Limit HTML syntax except when needed for formatting lists or code blocks.
4. Add in-line comments if needed for particularly complex or unusual logic.
