# Implementation Plan - Decompile, Update, and Deploy Workspace App

This plan outlines the steps to convert all `.class` files to `.java` files in the workspace directory `warehouse-1.0-SNAPSHOT`, implement the Excel/CSV export features (with `WGT_NET_ASY` column mapped), compile, and deploy it to the running Glassfish server.

## Proposed Changes

### 1. Decompile Class Files in Workspace
We will run `cfr.jar` to decompile all class files in the workspace class directory:
- Source: `C:\Users\Warehouse\Desktop\Glassfish_3.1.2.2\glassfish3\glassfish\domains\domain1\applications\warehouse-1.0-SNAPSHOT\WEB-INF\classes`
- Target output directory: Same as source.
- Command:
  ```powershell
  & "C:\Program Files\Java\jdk1.7.0_45\bin\java.exe" -jar "C:\Users\Warehouse\Desktop\warehouse-1.1-SNAPSHOT\devel_tools\cfr.jar" "C:\Users\Warehouse\Desktop\Glassfish_3.1.2.2\glassfish3\glassfish\domains\domain1\applications\warehouse-1.0-SNAPSHOT\WEB-INF\classes\mk\com\snt\kc\warehouse" --outputdir "C:\Users\Warehouse\Desktop\Glassfish_3.1.2.2\glassfish3\glassfish\domains\domain1\applications\warehouse-1.0-SNAPSHOT\WEB-INF\classes"
  ```

### 2. Implement Export Features in Workspace

#### [MODIFY] [WarehouseInventory.java](file:///C:/Users/Warehouse/Desktop/Glassfish_3.1.2.2/glassfish3/glassfish/domains/domain1/applications/warehouse-1.0-SNAPSHOT/WEB-INF/classes/mk/com/snt/kc/warehouse/domain/WarehouseInventory.java) (after decompilation)
- Map the missing column `WGT_NET_ASY` to the JPA entity:
  ```java
  @Column(name="WGT_NET_ASY")
  private BigDecimal wgtNetAsy;

  public BigDecimal getWgtNetAsy() {
      return this.wgtNetAsy;
  }
  public void setWgtNetAsy(BigDecimal wgtNetAsy) {
      this.wgtNetAsy = wgtNetAsy;
  }
  ```

#### [NEW] [ExcelExporterService.java](file:///C:/Users/Warehouse/Desktop/Glassfish_3.1.2.2/glassfish3/glassfish/domains/domain1/applications/warehouse-1.0-SNAPSHOT/WEB-INF/classes/mk/com/snt/kc/warehouse/boundary/ExcelExporterService.java)
- Create the exporter service to support:
  - `exportInventoryE` (CSV format)
  - `exportInventoryEXLS` (Excel XML SpreadsheetML format)
  - `previewInventoryE` (Preview of first 50 rows)

#### [NEW] [ExcelReportBean.java](file:///C:/Users/Warehouse/Desktop/Glassfish_3.1.2.2/glassfish3/glassfish/domains/domain1/applications/warehouse-1.0-SNAPSHOT/WEB-INF/classes/mk/com/snt/kc/warehouse/view/ExcelReportBean.java)
- Backing session bean to handle page initialization, data preview, CSV download, and Excel download actions.

#### [NEW] [excel_report.xhtml](file:///C:/Users/Warehouse/Desktop/Glassfish_3.1.2.2/glassfish3/glassfish/domains/domain1/applications/warehouse-1.0-SNAPSHOT/excel_report.xhtml)
- Create the XHTML report page for exporting the inventory to Excel and CSV, including preview functionality.

#### [MODIFY] [pretty-config.xml](file:///C:/Users/Warehouse/Desktop/Glassfish_3.1.2.2/glassfish3/glassfish/domains/domain1/applications/warehouse-1.0-SNAPSHOT/WEB-INF/pretty-config.xml)
- Add URL mappings for `excel_report`:
  ```xml
  <url-mapping id="excel_report"> 
      <pattern value="/excel" /> 
      <view-id value="/excel_report.xhtml" />
  </url-mapping>
  ```

#### [MODIFY] [Navigation.java](file:///C:/Users/Warehouse/Desktop/Glassfish_3.1.2.2/glassfish3/glassfish/domains/domain1/applications/warehouse-1.0-SNAPSHOT/WEB-INF/classes/mk/com/snt/kc/warehouse/view/util/Navigation.java) (after decompilation)
- Add the `excelReport` method:
  ```java
  public String excelReport() {
      return this.page("excel_report");
  }
  ```

#### [MODIFY] [shiro.ini](file:///C:/Users/Warehouse/Desktop/Glassfish_3.1.2.2/glassfish3/glassfish/domains/domain1/applications/warehouse-1.0-SNAPSHOT/WEB-INF/shiro.ini)
- Secure `/excel` and `/excel.xhtml` routes under Shiro authorization:
  ```ini
  /excel = user
  /excel.xhtml = user
  ```

#### [MODIFY] [auth_template.xhtml](file:///C:/Users/Warehouse/Desktop/Glassfish_3.1.2.2/glassfish3/glassfish/domains/domain1/applications/warehouse-1.0-SNAPSHOT/WEB-INF/templates/auth_template.xhtml)
- Add the "Excel" toolbar button:
  ```xml
  <p:commandButton icon="ui-icon-document" value="Excel" action="#{navigation.excelReport()}"/>
  ```

### 3. Compilation & Deployment
- We will compile all workspace Java sources to `WEB-INF/classes`.
- We will undeploy the current `whs` app from the running Glassfish.
- We will deploy the exploded workspace app `warehouse-1.0-SNAPSHOT` as context root `/warehouse`.

---

## Verification Plan

- Open `http://10.10.10.7/warehouse/` in a browser.
- Login as `admin` / `admin`.
- Navigate to the **Excel** tab.
- Choose a date range and click **Preview Data** to see the first 50 rows.
- Click **Download Excel File** and **Download CSV File** and verify that all 34 columns (including `WGT_NET_ASY` and `INSTANCEID`) are exported.
