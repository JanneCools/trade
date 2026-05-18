package be.ugent.ledc.core.binding.jdbc;

import be.ugent.ledc.core.binding.jdbc.agents.JDBCAgent;
import be.ugent.ledc.core.binding.jdbc.agents.postgres.PostgresAgent;

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
