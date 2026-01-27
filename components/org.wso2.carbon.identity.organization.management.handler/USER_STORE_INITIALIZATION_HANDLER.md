# User Store Initialization Handler

## Overview

The `UserStoreInitializationHandler` is an event handler that ensures user stores are fully initialized before organization creation APIs return. This prevents intermittent failures during user creation and role assignment operations in sub-organizations.

## Problem Statement

When creating a sub-organization using an M2M token (without user sharing), user stores like DEFAULT and AGENT may not be immediately available. This causes:
- User creation failures immediately after organization creation
- Role assignment failures even when user creation returns HTTP 201
- These issues occur intermittently (approximately 50% of the time)

## Solution

The handler listens to the `POST_ADD_ORGANIZATION` event and waits for configured user stores to be initialized before allowing the organization creation process to complete.

## Configuration

Add the following properties to `deployment.toml`:

```toml
[OrganizationUserStoreInitialization]
# Enable or disable the handler (default: true for Asgardeo, can be disabled for IS)
Enable = true

# Comma-separated list of user store names to wait for (default: DEFAULT,AGENT)
UserStores = "DEFAULT,AGENT"

# Maximum wait time in milliseconds (default: 60000 - 60 seconds)
WaitTime = 60000

# Wait interval between checks in milliseconds (default: 500)
WaitInterval = 500
```

## Deployment Scenarios

### Asgardeo (Default Configuration)
```toml
[OrganizationUserStoreInitialization]
Enable = true
UserStores = "DEFAULT,AGENT"
WaitTime = 60000
WaitInterval = 500
```

### WSO2 Identity Server (Handler Disabled)
```toml
[OrganizationUserStoreInitialization]
# Disable for IS as it uses primary userstore
Enable = false
```

### Custom User Stores
```toml
[OrganizationUserStoreInitialization]
Enable = true
UserStores = "CUSTOM1,CUSTOM2,CUSTOM3"
WaitTime = 90000
WaitInterval = 1000
```

## Behavior

1. **Root Organizations**: The handler is automatically skipped for root organizations (depth = 0)
2. **Sub-Organizations**: The handler waits for each configured user store to be initialized
3. **Timeout**: If a user store is not initialized within the configured wait time, an exception is thrown
4. **Logging**: Debug logs are written when waiting starts and when user stores are initialized

## Error Handling

If a user store is not initialized within the configured wait time:
- An `IdentityEventException` is thrown
- The organization creation process fails
- An error message is logged with details about which user store failed to initialize

## Performance Considerations

- The handler only runs for sub-organization creation events
- Each user store is checked sequentially to ensure proper initialization order
- The total maximum wait time = (number of user stores) × (configured wait time)
  - For default config (DEFAULT,AGENT with 60s wait time): maximum 120 seconds
  - In practice, user stores initialize much faster (typically < 5 seconds)
- Use appropriate wait time and interval values based on your deployment
- Consider reducing the number of user stores or wait time if organization creation time is critical

## Example Logs

```
DEBUG - UserStoreInitializationHandler - Waiting for user store 'DEFAULT' to be initialized for organization: sub-org-123
DEBUG - UserStoreInitializationHandler - User store 'DEFAULT' initialized successfully for organization: sub-org-123 (waited: 1500 ms)
DEBUG - UserStoreInitializationHandler - Waiting for user store 'AGENT' to be initialized for organization: sub-org-123
DEBUG - UserStoreInitializationHandler - User store 'AGENT' initialized successfully for organization: sub-org-123 (waited: 500 ms)
```

## Testing

The handler includes comprehensive unit tests covering:
- Successful initialization for sub-organizations
- Handler disabled configuration
- User stores initialized after delay
- Timeout scenarios
- Custom user store configurations
- Empty/null configuration handling
