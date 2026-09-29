package core.binding.jdbc;

import core.binding.jdbc.agents.JDBCAgent;
import core.binding.jdbc.agents.postgres.PostgresAgent;

public enum DBMS
{
    POSTGRESQL(new PostgresAgent());
    
    private final JDBCAgent agent;

    private DBMS(JDBCAgent agent)
    {
        this.agent = agent;
    }

    public JDBCAgent getAgent()
    {
        return agent;
    }
}
