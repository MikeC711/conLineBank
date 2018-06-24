# conLineBank
Casile's Online Bank ... including mySQL defs for now

## Data base Design
1. BankInfo
   1. RoutingNum    Long
   1. Name          Char(80)
   1. Street        Char(50)
   1. Addr2         Char(50)
   1. City          Char(30)
   1. State         Char(2)
   1. ZipCode       Char(9)
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
   1. Balance       Decimal(13,2)
   1. IntRate       Decimal(4, 3)
1. LoanAcct
   1. CustID        Long
   1. AcctID        Long
   1. AcctType      Char(2)
   1. Balance       Decimal(13,2)
   1. IntRate       Decimal(4,3)
   1. MonthlyPmt    Decimal(11,2)
   1. AutoDrftRte   Long
   1. AutoDrvtAcct  Long
1. AcctHistory
1. CustHistory

### Tran Types (right now, not doing DB gorp to make set up foreign keys and let DB manage integrity)
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

### Account History Records
1. DD Deposit into a Deposit Account
1. DI Deposit Acct Interest
1. AD ATM Deposit into a Deposit Account
1. AW ATM Withdrawal from a Deposit Account
1. DW Withdrawal from a Deposit Account
1. LP Loan Payment (in a loan account)
1. CC Check from a Checking Account
1. AP Auto Payment/Draft from a Checking or Savings Account
1. LR Interest rate change (Deposit or Loan Account)
1. LI Interest charge (deposit or loan account)

## Notes
1. ReLoad should be deterministic but parameterized
1. Reload should run thru the app (or sister app) and load any DB type used
1. App should call a DBInit in the appropriate module, then pass each DB call to that module.  JSON interface
1. Which DB should be an environment variable (defaulting to MYSQL)
1. Focusing first on all server side with REST calls. Then simple Angular 6 client in front of it.