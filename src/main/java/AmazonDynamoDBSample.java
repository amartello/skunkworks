import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.mortbay.util.ajax.JSON;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.ctlts.wfaas.data.orchestrate.query.Query;
import com.ctlts.wfaas.data.orchestrate.repository.OrchestrateTemplate;
import com.jcabi.dynamo.Credentials;

import io.ctl.robot.ops.lib.idm.model.UserAccount;
import io.ctl.robot.ops.lib.idm.model.UserAccount.LinkMethod;
import io.ctl.robot.ops.lib.idm.useraccount.model.CtlioUserAccount;

@SpringBootApplication
@ComponentScan("com")
public class AmazonDynamoDBSample {

	static DynamoDB dynamoDB = new DynamoDB(new AmazonDynamoDBClient(new ProfileCredentialsProvider()));

	static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	static String userAccountTableName = "UserAccount";

	static OrchestrateTemplate orchestrateTemplate;

	public static void main(String[] args) throws Exception {

		try {

			ApplicationContext ctx = SpringApplication.run(AmazonDynamoDBSample.class, args);
			 String[] beanNames = ctx.getBeanDefinitionNames();
		        Arrays.sort(beanNames);
		        for (String beanName : beanNames) {
		            System.out.println(beanName);
		        }
			OrchestrateTemplate template = new OrchestrateTemplate();

			String apiKey = "";
			template.setApiKey(apiKey);
			template.postConstruct();

			Credentials credentials = new Credentials.Simple("AWS key", "AWS secret");

			Query query = new Query("*");

			deleteTable(userAccountTableName);
			// EmployeeIdentity employee =
			// template.findById("000accf5-d2c0-4de8-9885-e3a0c34dcfae",
			// EmployeeIdentity.class, "Employee");

			// Parameter1: table name // Parameter2: reads per second //
			// Parameter3: writes per second // Parameter4/5: partition key and
			// data type
			// Parameter6/7: sort key and data type (if applicable)

			createTable(userAccountTableName, 10L, 5L, "id", "S");
			//findOne(userAccountTableName);
			Iterable<UserAccount> identities = template.findAll(query, UserAccount.class, "UserAccount");
			loadUserAccount(userAccountTableName, identities);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Program failed:");
			System.err.println(e.getMessage());
		}
		System.out.println("Success.");
	}

	private static void deleteTable(String tableName) {
		Table table = dynamoDB.getTable(tableName);
		try {
			System.out.println("Issuing DeleteTable request for " + tableName);
			table.delete();
			System.out.println("Waiting for " + tableName + " to be deleted...this may take a while...");
			table.waitForDelete();

		} catch (Exception e) {
			System.err.println("DeleteTable request failed for " + tableName);
			System.err.println(e.getMessage());
		}
	}

	private static void createTable(String tableName, long readCapacityUnits, long writeCapacityUnits,
			String partitionKeyName, String partitionKeyType) {

		createTable(tableName, readCapacityUnits, writeCapacityUnits, partitionKeyName, partitionKeyType,
				partitionKeyName, partitionKeyType);
	}

	private static void createTable(String tableName, long readCapacityUnits, long writeCapacityUnits,
			String partitionKeyName, String partitionKeyType, String sortKeyName, String sortKeyType) {

		try {

			ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
			keySchema.add(new KeySchemaElement().withAttributeName(partitionKeyName).withKeyType(KeyType.HASH)); // Partition
																													// key

			ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(
					new AttributeDefinition().withAttributeName(partitionKeyName).withAttributeType(partitionKeyType));

		
			CreateTableRequest request = new CreateTableRequest().withTableName(tableName).withKeySchema(keySchema)
					.withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(readCapacityUnits)
							.withWriteCapacityUnits(writeCapacityUnits));


			request.setAttributeDefinitions(attributeDefinitions);

			System.out.println("Issuing CreateTable request for " + tableName);
			Table table = dynamoDB.createTable(request);
			System.out.println("Waiting for " + tableName + " to be created...this may take a while...");
			table.waitForActive();

		} catch (Exception e) {
			System.err.println("CreateTable request failed for " + tableName);
			System.err.println(e.getMessage());
		}
	}

	private static void loadUserAccount(String tableName, Iterable<UserAccount> identities) {

		Table table = dynamoDB.getTable(tableName);

		try {

			System.out.println("Adding data to " + tableName);

			for (UserAccount a : identities) {
				if (a instanceof CtlioUserAccount) {
					CtlioUserAccount account = (CtlioUserAccount) a;

					Class accountClass = account.getClass();
					
					
					Item user = new Item().withPrimaryKey("id", account.getId())
							.withString("username", account.getUsername())
							.withString("system", account.getSystem().name()).withList("emails", account.getEmails())
							.withString("status", account.getStatus().name()).withBoolean("active", account.isActive())
							.withString("role", account.getRole())
							.withString("linkMethod",
									account.getLinkMethod() != null ? account.getLinkMethod().name()
											: LinkMethod.CREATE.name())
							.withString("secondaryUsername", account.getSecondaryUsername())
							.withJSON("employeeName", JSON.toString(account.getEmployeeName()))
							.withString("employeeId",
									account.getEmployeeId() != null ? account.getEmployeeId() : "unknown")
							.withString("primaryEmail",
									account.getPrimaryEmail() != null ? account.getPrimaryEmail() : "unknown");
					/*
					Item user = new Item();
					Field[] fields = accountClass.getDeclaredFields();
					for(int i = 0; i<fields.length;i++){
						Field f = fields[i];
						if(Modifier.isPrivate(f.getModifiers())){
							f.setAccessible(true);
						}
						if(f.getName().equals("id") || ArrayUtils.contains(f.getDeclaredAnnotations(), "ID")) {
							user.withPrimaryKey("id",f.get(account).toString());
						}else{
							Object val = f.get(account);
							if(val != null && val instanceof String){								
								user.withString(f.getName(),val.toString());
							}else if(val != null){
								user.withJSON(f.getName(), JSON.toString(val));
							}
						}
					}*/
					table.putItem(user);
					System.out.println("Added user :" + account.getUsername());
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Failed to create item in " + tableName);
			System.err.println(e.getMessage());
		}
	}

	private static void findOne(String tableName) {
		Table table = dynamoDB.getTable(tableName);

		QuerySpec spec = new QuerySpec().withKeyConditionExpression("username = :username")
				.withValueMap(new ValueMap().withString(":username", "jwoffice@ctl.io"));

		ItemCollection<QueryOutcome> items = table.query(spec);

		Iterator<Item> iterator = items.iterator();
		Item item = null;
		while (iterator.hasNext()) {
			item = iterator.next();
			System.out.println("------------------------");
			System.out.println(item.toJSONPretty());
			System.out.println("------------------------");
		}
	}

}