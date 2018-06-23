# conLineBank
Casile's Online Bank ... including mySQL defs for now

## Data base Design
1. Customer
   1. FirstName     Char(30)
   1. LastName      Char(30)
   1. CustID        Long
   1. Street        Char(50)
   1. Addr2         Char(50)
   1. City          Char(30)
   1. State         Char(2)
   1. ZipCode       Char(9)
1. DepositAcct
   1. CustID        Long
   1. AcctID        Long
   1. AcctType      Char(2)
1. LoanAcct
1. AcctHistory
1. CustHistory

### Tran Types
1. SD - Standard Deposit
1. MD - Mobile Deposit
1. SW - Standard withdrawal
1. XF - Transfer

### Account Types
1. CH - Checking
1. SA - Savings
1. ML - Mortgage Loan
1. AL - Automobile Loan
1. PL - Personal Loan
1. CC - Credit Card

### Databases to support early
1. MYSQL
1. Mongo
1. Oracle
1. DB2

## Notes
1. ReLoad should be deterministic but parameterized
1. Reload should run thru the app (or sister app) and load any DB type used