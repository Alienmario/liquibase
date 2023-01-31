package liquibase.command.core;

import liquibase.command.*;
import liquibase.command.providers.ReferenceDatabase;
import liquibase.database.Database;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.ObjectChangeFilter;
import liquibase.diff.output.StandardObjectChangeFilter;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.structure.DatabaseObject;

import java.util.Arrays;
import java.util.List;

public class PreCompareCommandStep extends AbstractCommandStep {

    protected static final String[] COMMAND_NAME = {"preCompareCommandStep"};
    public static final CommandArgumentDefinition<String> EXCLUDE_OBJECTS_ARG;
    public static final CommandArgumentDefinition<String> INCLUDE_OBJECTS_ARG;
    public static final CommandArgumentDefinition<String> SCHEMAS_ARG;
    public static final CommandArgumentDefinition<String> DIFF_TYPES_ARG;

    public static final CommandArgumentDefinition<String> REFERENCE_SCHEMAS_ARG;
    public static final CommandArgumentDefinition<String> OUTPUT_SCHEMAS_ARG;

    static {
        CommandBuilder builder = new CommandBuilder(COMMAND_NAME);
        EXCLUDE_OBJECTS_ARG = builder.argument("excludeObjects", String.class)
                .description("Objects to exclude from diff").build();
        INCLUDE_OBJECTS_ARG = builder.argument("includeObjects", String.class)
                .description("Objects to include in diff").build();
        SCHEMAS_ARG = builder.argument("schemas", String.class)
                .description("Schemas to include in diff").build();
        DIFF_TYPES_ARG = builder.argument("diffTypes", String.class)
                .description("Types of objects to compare").build();

        REFERENCE_SCHEMAS_ARG = builder.argument("referenceSchemas", String.class)
                .description("Schemas names on reference database to use in diff. This is a CSV list.").build();
        OUTPUT_SCHEMAS_ARG = builder.argument("outputSchemas", String.class)
                .description("Output schemas names. This is a CSV list.").build();
    }

    @Override
    public List<Class<?>> requiredDependencies() {
        return Arrays.asList(Database.class, ReferenceDatabase.class);
    }

    @Override
    public List<Class<?>> providedDependencies() {
        return Arrays.asList(PreCompareCommandStep.class);
    }

    @Override
    public String[][] defineCommandNames() {
        return new String[][] { COMMAND_NAME };
    }

    @Override
    public void run(CommandResultsBuilder resultsBuilder) throws Exception {
        CommandScope commandScope = resultsBuilder.getCommandScope();
        Database targetDatabase = (Database) commandScope.getDependency(Database.class);
        ObjectChangeFilter objectChangeFilter = this.getObjectChangeFilter(commandScope);
        CompareControl compareControl = this.getcompareControl(commandScope, targetDatabase);
        Class<? extends DatabaseObject>[] snapshotTypes = DiffCommandStep.parseSnapshotTypes(commandScope.getArgumentValue(DIFF_TYPES_ARG));

        commandScope
                .addArgumentValue(DiffCommandStep.COMPARE_CONTROL_ARG, compareControl)
                .addArgumentValue(DiffCommandStep.OBJECT_CHANGE_FILTER_ARG, objectChangeFilter)
                .addArgumentValue(DiffCommandStep.SNAPSHOT_TYPES_ARG, snapshotTypes);
    }

    private ObjectChangeFilter getObjectChangeFilter(CommandScope commandScope) {
        String excludeObjects = commandScope.getArgumentValue(EXCLUDE_OBJECTS_ARG);
        String includeObjects = commandScope.getArgumentValue(INCLUDE_OBJECTS_ARG);

        if ((excludeObjects != null) && (includeObjects != null)) {
            throw new UnexpectedLiquibaseException(
                    String.format(coreBundle.getString("cannot.specify.both"),
                            EXCLUDE_OBJECTS_ARG.getName(), INCLUDE_OBJECTS_ARG.getName()));
        }

        ObjectChangeFilter objectChangeFilter = null;
        if (excludeObjects != null) {
            objectChangeFilter = new StandardObjectChangeFilter(StandardObjectChangeFilter.FilterType.EXCLUDE,
                    excludeObjects);
        }
        if (includeObjects != null) {
            objectChangeFilter = new StandardObjectChangeFilter(StandardObjectChangeFilter.FilterType.INCLUDE,
                    includeObjects);
        }

        return objectChangeFilter;
    }

    private CompareControl getcompareControl(CommandScope commandScope, Database database) {
        CompareControl.SchemaComparison[] finalSchemaComparisons = CompareControl.computeSchemas(
                commandScope.getArgumentValue(SCHEMAS_ARG),
                commandScope.getArgumentValue(REFERENCE_SCHEMAS_ARG),
                commandScope.getArgumentValue(OUTPUT_SCHEMAS_ARG),
                commandScope.getArgumentValue(DbUrlConnectionCommandStep.DEFAULT_CATALOG_NAME_ARG),
                commandScope.getArgumentValue(DbUrlConnectionCommandStep.DEFAULT_SCHEMA_NAME_ARG),
                commandScope.getArgumentValue(ReferenceDbUrlConnectionCommandStep.REFERENCE_DEFAULT_CATALOG_NAME_ARG),
                commandScope.getArgumentValue(ReferenceDbUrlConnectionCommandStep.REFERENCE_DEFAULT_SCHEMA_NAME_ARG),
                database).finalSchemaComparisons;

        return new CompareControl(finalSchemaComparisons, commandScope.getArgumentValue(DIFF_TYPES_ARG));
    }

    @Override
    public void adjustCommandDefinition(CommandDefinition commandDefinition) {
        if (commandDefinition.getPipeline().size() == 1) {
            commandDefinition.setInternal(true);
        }
    }
}