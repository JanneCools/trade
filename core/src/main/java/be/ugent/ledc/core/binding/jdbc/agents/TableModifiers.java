package be.ugent.ledc.core.binding.jdbc.agents;

public class TableModifiers
{
    private final boolean addColumns;
    private final boolean dropColumns;
    private final boolean changeDatatypes;
    private final boolean changeForeignKeys;
    private final boolean changeUniqueConstraints;
    
    private TableModifiers(TableModifiersBuilder builder)
    {
        this.addColumns = builder.addColumns;
        this.dropColumns = builder.dropColumns;
        this.changeDatatypes = builder.changeDatatypes;
        this.changeForeignKeys = builder.changeForeignKeys;
        this.changeUniqueConstraints = builder.changeUniqueConstraints;
    }

    public boolean isAddColumns()
    {
        return addColumns;
    }

    public boolean isDropColumns()
    {
        return dropColumns;
    }

    public boolean isChangeDatatypes()
    {
        return changeDatatypes;
    }

    public boolean isChangeForeignKeys()
    {
        return changeForeignKeys;
    }

    public boolean isChangeUniqueConstraints()
    {
        return changeUniqueConstraints;
    }
    
    public boolean isModifiable()
    {
        return isAddColumns() || isDropColumns() || isChangeDatatypes() || isChangeForeignKeys() || isChangeUniqueConstraints();
    }
    
    public static class TableModifiersBuilder
    {
        private boolean addColumns;
        private boolean dropColumns;
        private boolean changeDatatypes;
        private boolean changeForeignKeys;
        private boolean changeUniqueConstraints;

        public TableModifiersBuilder(){}
        
        public TableModifiersBuilder allModifiable(boolean allModifiable)
        {
            addColumns(allModifiable);
            dropColumns(allModifiable);
            changeDatatypes(allModifiable);
            changeForeignKeys(allModifiable);
            changeUniqueConstraints(allModifiable);
            return this;
        }
        
        public TableModifiersBuilder addColumns(boolean addColumns)
        {
            this.addColumns = addColumns;
            return this;
        }
        
        public TableModifiersBuilder dropColumns(boolean dropColumns)
        {
            this.dropColumns = dropColumns;
            return this;
        }
        
        public TableModifiersBuilder changeDatatypes(boolean changeDatatypes)
        {
            this.changeDatatypes = changeDatatypes;
            return this;
        }
        
        public TableModifiersBuilder changeForeignKeys(boolean changeForeignKeys)
        {
            this.changeForeignKeys = changeForeignKeys;
            return this;
        }
        
        public TableModifiersBuilder changeUniqueConstraints(boolean changeUniqueConstraints)
        {
            this.changeUniqueConstraints = changeUniqueConstraints;
            return this;
        }

        public TableModifiers build()
        {
            return new TableModifiers(this);
        }
    }
}