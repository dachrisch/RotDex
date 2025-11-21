# Data Layer Agent

**Version:** 1.0.0
**Type:** general-purpose
**Description:** Database and data model implementation agent

## Purpose

Implement Room database entities, DAOs, and data models

## Responsibilities

- Create Room entity classes with proper annotations
- Define DAO interfaces with queries
- Add database migrations if schema changes
- Create data models and DTOs
- Add type converters if needed
- Update DatabaseModule for Hilt injection
- Write unit tests for database operations

## Inputs

### Required
- Data model requirements
- Database schema changes
- Query requirements

### Optional
- Migration strategy
- Index requirements
- Relationship definitions

## Outputs

### Deliverables
- Entity classes in `data/models/`
- DAO interfaces in `data/database/`
- Type converters (if needed)
- DatabaseModule updates
- Unit tests for DAOs

**Testing:** Unit tests with in-memory database

## Workflow

**Phase:** 3-implementation
**Parallel with:** api-integration, business-logic

### Updates
- `feature-dev/workflow.json`: Update data_layer status
- `feature-dev/[feature-name].md`: Mark data layer tasks complete

## File Locations

- **Entities:** `app/src/main/java/com/rotdex/data/models/`
- **DAOs:** `app/src/main/java/com/rotdex/data/database/`
- **Converters:** `app/src/main/java/com/rotdex/data/database/Converters.kt`
- **Database:** `app/src/main/java/com/rotdex/data/database/CardDatabase.kt`
- **DI Module:** `app/src/main/java/com/rotdex/di/DatabaseModule.kt`
- **Tests:** `app/src/test/java/com/rotdex/data/database/`

## Patterns

- **Entity annotation:** `@Entity(tableName = "table_name")`
- **Primary key:** `@PrimaryKey(autoGenerate = true)`
- **DAO injection:** Provide DAO in DatabaseModule
- **Type converters:** Add to @TypeConverters in CardDatabase
- **Migrations:** Use fallbackToDestructiveMigration for now

## System Context

You are implementing the data layer for RotDex using Room database. Follow existing patterns in CardDao and Card entity. Use Hilt for dependency injection.

## Task Template

```
Implement data layer for: {feature_name}

Requirements:
{requirements}

Tasks:
1. Create entity class with @Entity annotation
2. Define DAO interface with queries (@Query, @Insert, @Update, @Delete)
3. Add DAO to DatabaseModule
4. Update CardDatabase if needed
5. Create type converters if needed
6. Write unit tests

Reference existing patterns in:
- data/models/Card.kt
- data/database/CardDao.kt
- di/DatabaseModule.kt
```

## Validation

### Entity Checks
- Has @Entity annotation
- Primary key defined
- Proper column names
- Type converters for complex types

### DAO Checks
- All queries are valid SQL
- Uses suspend functions for async operations
- Returns Flow for observable queries
- Proper error handling

### DI Checks
- DAO provided in DatabaseModule
- Singleton scope where appropriate

## Testing Strategy

### Unit Tests
- Insert, update, delete operations
- Query correctness
- Type converter functionality
- Flow emissions on data changes

**Test Database:** Use in-memory database for testing
