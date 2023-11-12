package ua.nure.st.kpp.example.demo.dao;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ua.nure.st.kpp.example.demo.dao.implementation.mongodb.MongoDbDaoFactory;
import ua.nure.st.kpp.example.demo.dao.implementation.mysql.MySqlDAOFactory;

import javax.naming.ConfigurationException;

@Component
public class DAOFactory implements Factory {
    //Pattern "Strategy"
    private final Factory factory;

    @Autowired
    public DAOFactory(@Value("${database.type}") String databaseType,
                      MySqlDAOConfig mySqlDAOConfig,
                      MongoDbDAOConfig mongoDbDAOConfig) {
        if (TypeDAO.MySQL.name().equalsIgnoreCase(databaseType)) {
            this.factory = new MySqlDAOFactory(mySqlDAOConfig);
            return;
        }
        if (TypeDAO.MONGODB.name().equalsIgnoreCase(databaseType)) {
            this.factory = new MongoDbDaoFactory(mongoDbDAOConfig);
            return;
        }

        throw new RuntimeException(new ConfigurationException("Unknown DAO type: " + mySqlDAOConfig));
    }

    @Bean
    @Scope("singleton")
    public ItemDAO createItemDAO() {
        return factory.createItemDAO();
    }

    @Bean
    @Scope("singleton")
    public CompanyDAO createCompanyDAO() {
        return factory.createCompanyDAO();
    }

    @Bean
    @Scope("singleton")
    public IncomeJournalDAO createIncomeJournalDAO() {
        return factory.createIncomeJournalDAO();
    }

    @Bean
    @Scope("singleton")
    public OutcomeJournalDAO createOutcomeJournalDAO() {
        return factory.createOutcomeJournalDAO();
    }

}
