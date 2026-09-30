package by.yurnerix.msaccountreservation.integration.dataset;

import com.github.database.rider.core.api.dataset.DataSetProvider;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.datatype.DataType;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public class ClientQueryCountDataSetProvider implements DataSetProvider {

    public static final String SEARCH_LAST_NAME = "UPG9QueryCount";
    public static final int SEARCH_CLIENTS_COUNT = 11;

    private static final long MDM_ID_BASE = 9_900_000_000L;

    @Override
    public IDataSet provide() throws DataSetException {
        DefaultTable clients = createTable(
                "client",
                "id",
                "mdm_id",
                "first_name",
                "last_name",
                "middle_name",
                "citizenship",
                "client_type",
                "document_number",
                "document_series",
                "document_type",
                "status"
        );

        DefaultTable accounts = createTable(
                "account",
                "id",
                "status_id",
                "client_id",
                "account_type",
                "currency_code"
        );

        for (int index = 0; index < SEARCH_CLIENTS_COUNT; index++) {
            long clientNumber = index + 1L;

            String firstName = String.format(Locale.ROOT, "Client%02d", index);

            String status = index % 2 == 0 ? "ACTIVE" : "BLOCKED";

            addClient(clients, clientNumber, firstName, SEARCH_LAST_NAME, status);

            if (index % 2 == 0) {
                addAccounts(accounts, clientNumber, 5);
            }
        }


        addClient(clients, 12, "Deleted", SEARCH_LAST_NAME, "DELETED");

        addClient(clients, 13, "Other", "OtherQueryGroup", "ACTIVE");

        addClient(clients, 100, "WithoutAccounts", "DetailsQueryGroup", "ACTIVE");

        addClient(clients, 101, "WithOneAccount", "DetailsQueryGroup", "ACTIVE");

        addAccounts(accounts, 101, 1);

        return new DefaultDataSet(new ITable[]{clients, accounts});
    }

    public static UUID clientId(long number) {
        return new UUID(0L, number);
    }

    private DefaultTable createTable(String name, String... columnNames) {
        Column[] columns = Arrays.stream(columnNames)
                .map(column -> new Column(column, DataType.UNKNOWN))
                .toArray(Column[]::new);

        return new DefaultTable(name, columns);
    }

    private void addClient(DefaultTable clients, long number, String firstName, String lastName, String status) throws DataSetException {
        clients.addRow(new Object[]{
                clientId(number).toString(),
                MDM_ID_BASE + number,
                firstName,
                lastName,
                "Тестович",
                "Россия",
                "INDIVIDUAL",
                Long.toString(number),
                "1234",
                "PASSPORT",
                status
        });
    }

    private void addAccounts(DefaultTable accounts, long clientNumber, int count) throws DataSetException {

        for (int statusId = 1; statusId <= count; statusId++) {
            accounts.addRow(new Object[]{
                    new UUID(clientNumber, statusId).toString(),
                    statusId,
                    clientId(clientNumber).toString(),
                    "CURRENT",
                    "RUB"
            });
        }
    }

}
