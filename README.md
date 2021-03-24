## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus .
docker run -p 127.0.0.1:7000:7000 antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

## Solution
The solution took approximately x hours to set up, implement, test and document.
### Basic functionality
Upon initialization, a timer is set to process all pending invoices every month on the 1st.

There are two endpoints to process building through, ``/rest/v1/billing/process`` and ``/rest/v1/billing/process/id``,
one for processing all pending invoices and one to process a single pending invoice. The first endpoint runs the same
function as the monthly process to allow for manual updates.

Both processes tries to process a pending payment through the payment provider and marks the invoice as either Paid,
in case the payment goes through, Delinquent, in case the payment is rejected, or Error, in case an error occurred.
This is to more easily expand the system to handle different business cases as described in the Future: Reporting
Framework section.

A simple retry strategy has been implemented for each call to the payment provider. It would be beneficial to
restructure the code handling calls to the payment provider to do concurrent calls in order to speed up processing,
especially in case of problems with the payment provider, however, I didn't implement this in the current solution.

### Future: Reporting Framework
The solution as it is now only looks at whether the status is pending to determine whether a charge should be made.
I added two extra status types, delinquent and error, to show what happened when a charge failed, which could be
used for creating custom reports for a customer support or sales rep to follow up on. It would be beneficial as well
when building out the framework to add more details to the error status.

A reporting framework could then contain two different paths, depending on the outcome:

* A report with Delinquent payments to be contacted by a representative in order to find out a payment plan with  a
  system to support that.
* A report with payments that incurred an Error, which could be manually investigated. Depending on the error,
  possible business logic could include:
  * For Network Errors, investigations into the payment provider and possible downtime reasons or other technical
    errors are initiated by some technical staff. When the problem has been fixed, the unpaid invoices can be
    re-processed in bulk.
  * A Customer Not Found error would either indicate that we have an error in our system, such as a user having been
    deleted without proper clean-up of their invoices. This would require a manual assessment of the reason for the
    error and proper next steps, possibly in collaboration with Sales and Engineering.
  * As for Currency Mismatch errors, this can happen when a customer changes their currency, yet has unpaid invoices
    in the system. The right solution here would be to create a system that would allow for the invalidation of an
    invoice followed by the creation of a new invoice in the new currency.
    
Since we're dealing with finances, it's generally important that we're diligent not to change or delete information
around invoices.

To support a proper reporting framework, I would also ensure we expose the date of changes on invoices so that these
can be pulled monthly and used for analytics.
