package com.demo.ai.util;

public class TallyXMLBuilder {

    public static String vendorXML(String companyName, String vendorId, String vendorName) {
        return """
<ENVELOPE>
 <HEADER><TALLYREQUEST>Import Data</TALLYREQUEST></HEADER>
 <BODY>
  <IMPORTDATA>
   <REQUESTDESC>
    <REPORTNAME>All Masters</REPORTNAME>
    <STATICVARIABLES><SVCURRENTCOMPANY>%s</SVCURRENTCOMPANY></STATICVARIABLES>
   </REQUESTDESC>
   <REQUESTDATA>
    <TALLYMESSAGE xmlns:UDF="TallyUDF">
     <LEDGER NAME="%s" ACTION="Create">
      <NAME>%s</NAME>
      <PARENT>Sundry Creditors</PARENT>
      <OPENINGBALANCE>0</OPENINGBALANCE>
      <ISBILLWISEON>Yes</ISBILLWISEON>
     </LEDGER>
    </TALLYMESSAGE>
   </REQUESTDATA>
  </IMPORTDATA>
 </BODY>
</ENVELOPE>
""".formatted(companyName, vendorName, vendorName);
    }

    public static String invoiceXML(String companyName, String vendorName, double amount) {
        return """
    <ENVELOPE>
     <HEADER><TALLYREQUEST>Import Data</TALLYREQUEST></HEADER>
     <BODY>
      <IMPORTDATA>
       <REQUESTDESC>
        <REPORTNAME>Vouchers</REPORTNAME>
        <STATICVARIABLES><SVCURRENTCOMPANY>%s</SVCURRENTCOMPANY></STATICVARIABLES>
       </REQUESTDESC>
       <REQUESTDATA>
        <TALLYMESSAGE xmlns:UDF="TallyUDF">
         <VOUCHER VCHTYPE="Purchase" ACTION="Create">
          <DATE>20260401</DATE> 
          <VOUCHERTYPENAME>Purchase</VOUCHERTYPENAME>
          <PARTYLEDGERNAME>%s</PARTYLEDGERNAME>
          
          <ALLLEDGERENTRIES.LIST>
           <LEDGERNAME>%s</LEDGERNAME>
           <ISDEEMEDPOSITIVE>No</ISDEEMEDPOSITIVE>
           <AMOUNT>%.2f</AMOUNT> 
          </ALLLEDGERENTRIES.LIST>

          <ALLLEDGERENTRIES.LIST>
           <LEDGERNAME>Purchase</LEDGERNAME>
           <ISDEEMEDPOSITIVE>Yes</ISDEEMEDPOSITIVE>
           <AMOUNT>-%.2f</AMOUNT>
          </ALLLEDGERENTRIES.LIST>
         </VOUCHER>
        </TALLYMESSAGE>
       </REQUESTDATA>
      </IMPORTDATA>
     </BODY>
    </ENVELOPE>
    """.formatted(companyName, vendorName, vendorName, amount, amount);
    }
}