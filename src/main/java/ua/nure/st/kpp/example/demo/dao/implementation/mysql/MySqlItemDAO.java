package ua.nure.st.kpp.example.demo.dao.implementation.mysql;

import ua.nure.st.kpp.example.demo.dao.DAOException;
import ua.nure.st.kpp.example.demo.dao.ItemDAO;
import ua.nure.st.kpp.example.demo.dao.implementation.mysql.util.MySqlConnectionUtils;
import ua.nure.st.kpp.example.demo.entity.Item;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

public class MySqlItemDAO implements ItemDAO {
    private final MySqlConnectionUtils mySqlConnectionUtils;

    public MySqlItemDAO(MySqlConnectionUtils mySqlConnectionUtils) {

        this.mySqlConnectionUtils = mySqlConnectionUtils;
    }

    private static class Query {
        public static final String GET_ALL_ITEMS = "SELECT items.id, vendor, name, unit, weight, amount, reserve_rate FROM items JOIN units ON items.unit_id = units.id WHERE amount IS NOT NULL  ORDER BY id;";
        public static final String GET_ITEM_BY_VENDOR = "SELECT items.id, vendor, name, unit, weight, amount, reserve_rate FROM items JOIN units ON items.unit_id = units.id WHERE vendor = ?";
        public static final String GET_ITEM_BY_ID = "SELECT items.id, vendor, name, unit, weight, amount, reserve_rate FROM items JOIN units ON items.unit_id = units.id WHERE items.id = ?";

        public static final String INSERT_ITEM = "INSERT INTO items(vendor, name, unit_id, weight, amount, reserve_rate) VALUES(?,?,?,?,?,?);";
        public static final String GET_UNIT_ID_BY_VALUE = "SELECT id FROM units WHERE unit=?;";
        public static final String INSERT_UNIT = "INSERT INTO units(unit) VALUES(?);";
        public static final String UPDATE_ITEM_QUANTITY_BY_ID = "UPDATE items SET amount = ? WHERE id = ?;";
        public static final String UPDATE_ITEM = "UPDATE items SET vendor = ?, name = ?,unit_id = ?, weight = ?, reserve_rate = ? WHERE id = ?;";
        public static final String SET_AMOUNT_NULL = "UPDATE items SET amount = NULL WHERE id = ?;";
        public static final String GET_ALL_ITEMS_ID = "SELECT id FROM items WHERE amount IS NOT NULL ORDER BY id";
        public static final String GET_ALL_ITEMS_BY_NAME_AND_AMOUNT = "SELECT items.id, vendor, name, unit, weight, amount, reserve_rate FROM items JOIN units ON items.unit_id = units.id WHERE name LIKE ? AND amount >= ? AND  amount <=?";
    }
    private static class Procedure{
        public static final String GET_ITEM_BY_NAME = "CALL get_all_items_by_name(?);";
    }

    @Override
    public Item create(Item item) throws DAOException {
        try (Connection connection = mySqlConnectionUtils.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(Query.INSERT_ITEM, Statement.RETURN_GENERATED_KEYS)) {
            int unitId = getUnitId(item, connection);
            mapInsertStatement(preparedStatement, item, unitId);

            preparedStatement.execute();
            ResultSet keys = preparedStatement.getGeneratedKeys();
            keys.next();
            return readById(keys.getString(1));
        } catch (SQLException exception) {
            throw new DAOException(exception);
        }
    }

    private void mapInsertStatement(PreparedStatement preparedStatement, Item item, int unitId) throws SQLException {
        int index = 1;
        preparedStatement.setString(index++, item.getVendor());
        preparedStatement.setString(index++, item.getName());
        preparedStatement.setInt(index++, unitId);
        preparedStatement.setBigDecimal(index++, item.getWeight());
        preparedStatement.setInt(index++, item.getAmount());
        preparedStatement.setInt(index, item.getReserveRate());
    }

    @Override
    public boolean updateQuantity(String id, int quantity) throws DAOException {
        try (Connection connection = mySqlConnectionUtils.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(Query.UPDATE_ITEM_QUANTITY_BY_ID)) {
            int index = 1;
            preparedStatement.setInt(index++, quantity);
            preparedStatement.setInt(index, Integer.parseInt(id));

            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DAOException(exception);
        }
    }

    @Override
    public boolean update(String id, Item item) throws DAOException {
        try (Connection connection = mySqlConnectionUtils.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(Query.UPDATE_ITEM)) {
            int unitId = getUnitId(item, connection);

            mapUpdateStatement(preparedStatement, Integer.parseInt(id), item, unitId);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DAOException(exception);
        }
    }

    private void mapUpdateStatement(PreparedStatement preparedStatement, int id, Item item, int unitId) throws SQLException {
        int index = 1;
        preparedStatement.setString(index++, item.getVendor());
        preparedStatement.setString(index++, item.getName());
        preparedStatement.setInt(index++, unitId);
        preparedStatement.setBigDecimal(index++, item.getWeight());
        preparedStatement.setInt(index++, item.getReserveRate());
        preparedStatement.setInt(index, id);
    }

    private int getUnitId(Item item, Connection connection) throws SQLException {
        int unitId = -1;
        try (PreparedStatement preparedStatement = connection.prepareStatement(Query.GET_UNIT_ID_BY_VALUE)) {
            preparedStatement.setString(1, item.getUnit());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    unitId = resultSet.getInt("id");
                } else {
                    try (PreparedStatement preparedStatement1 = connection.prepareStatement(Query.INSERT_UNIT, Statement.RETURN_GENERATED_KEYS)) {
                        preparedStatement1.setString(1, item.getUnit());
                        preparedStatement1.execute();
                        try (ResultSet resultSet1 = preparedStatement1.getGeneratedKeys()) {
                            if (resultSet1.next()) {
                                unitId = resultSet1.getInt(1);
                            }
                        }
                    }
                }
            }
        }
        return unitId;
    }

    @Override
    public List<Item> readAll() throws DAOException {
        List<Item> items = new LinkedList<>();
        try (Connection connection = mySqlConnectionUtils.getConnection();
             Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(Query.GET_ALL_ITEMS)) {
                while (resultSet.next()) {
                    Item item = mapItem(resultSet);
                    items.add(item);
                }
            }
        } catch (SQLException exception) {
            throw new DAOException(exception);
        }
        return items;
    }

    @Override
    public List<Item> readByNameAndAmount(String name, int minAmount, int maxAmount) throws DAOException {
        List<Item> items = new LinkedList<>();
        try (Connection connection = mySqlConnectionUtils.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(Query.GET_ALL_ITEMS_BY_NAME_AND_AMOUNT)) {
            preparedStatement.setString(1,'%' + name + '%');
            preparedStatement.setInt(2,minAmount);
            preparedStatement.setInt(3,maxAmount);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Item item = mapItem(resultSet);
                    items.add(item);
                }
            }
        } catch (SQLException exception) {
            throw new DAOException(exception);
        }
        return items;
    }

    @Override
    public Item readByVendor(String vendor) throws DAOException {
        try (Connection connection = mySqlConnectionUtils.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(Query.GET_ITEM_BY_VENDOR)) {
            preparedStatement.setString(1, vendor);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapItem(resultSet);
                }
            }
        } catch (SQLException exception) {
            throw new DAOException(exception);
        }
        return null;
    }

    @Override
    public Item readById(String id) throws DAOException {
        try (Connection connection = mySqlConnectionUtils.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(Query.GET_ITEM_BY_ID)) {
            preparedStatement.setInt(1, Integer.parseInt(id));
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapItem(resultSet);
                }
            }
        } catch (SQLException exception) {
            throw new DAOException(exception);
        }
        return null;
    }

    @Override
    public boolean delete(String id) throws DAOException {
        try (Connection connection = mySqlConnectionUtils.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(Query.SET_AMOUNT_NULL)) {
            preparedStatement.setInt(1, Integer.parseInt(id));
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DAOException(exception);
        }
    }

    @Override
    public List<String> readAllAvailableId() throws DAOException {
        List<String> idList = new LinkedList<>();
        try (Connection connection = mySqlConnectionUtils.getConnection();
             Statement statement = connection.createStatement()){
            try(ResultSet resultSet = statement.executeQuery(Query.GET_ALL_ITEMS_ID)){
                while(resultSet.next()){
                    idList.add(String.valueOf(resultSet.getInt("id")));
                }
            }
        } catch (SQLException exception) {
            throw new DAOException(exception);
        }
        return idList;

    }

    @Override
    public List<Item> readAllByName(String name) throws DAOException {
        List<Item> items = new LinkedList<>();
        try (Connection connection = mySqlConnectionUtils.getConnection();
             CallableStatement callableStatement = connection.prepareCall(Procedure.GET_ITEM_BY_NAME)) {
            callableStatement.setString("name",name);
            try (ResultSet resultSet = callableStatement.executeQuery()) {
                while (resultSet.next()) {
                    Item item = mapItem(resultSet);
                    items.add(item);
                }
            }
        } catch (SQLException exception) {
            throw new DAOException(exception);
        }
        return items;
    }

    private Item mapItem(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String vendor = resultSet.getString("vendor");
        String name = resultSet.getString("name");
        String unit = resultSet.getString("unit");
        BigDecimal weight = resultSet.getBigDecimal("weight");
        int amount = resultSet.getInt("amount");
        int reserveRate = resultSet.getInt("reserve_rate");
        return new Item.Builder<>()
                .id(String.valueOf(id))
                .name(name)
                .vendor(vendor)
                .weight(weight)
                .unit(unit)
                .amount(amount)
                .reserveRate(reserveRate)
                .build();
    }
}
