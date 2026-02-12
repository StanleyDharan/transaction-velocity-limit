MVN := mvn
REPORT := target/site/surefire-report.html

.PHONY: build test unit-test integration-test run clean

## Build the project (compile + package, skip tests)
build:
	$(MVN) package -DskipTests

## Run all tests and generate HTML report
test:
	$(MVN) test
	@$(MAKE) --no-print-directory _report

## Run unit tests only and generate HTML report
unit-test:
	$(MVN) test -Dsurefire.excludes="com.venn.velocitylimits.integration.**"
	@$(MAKE) --no-print-directory _report

## Run integration tests only and generate HTML report
integration-test:
	$(MVN) test -Dsurefire.includes="com.venn.velocitylimits.integration.**"
	@$(MAKE) --no-print-directory _report

## Start the service
run:
	$(MVN) spring-boot:run

## Remove build artifacts and SQLite DB
clean:
	$(MVN) clean
	rm -f velocity-limits.db velocity-limits.db-wal velocity-limits.db-shm

## Generate HTML report and print summary
_report:
	$(MVN) surefire-report:report-only -q
	@echo ""
	@echo "========================================"
	@echo "  TEST REPORT"
	@echo "========================================"
	@awk '\
		/<testcase / { \
			total++; status="PASS"; name=""; cls=""; \
			s=$$0; \
			i=index(s, "name=\""); \
			if (i > 0) { s2=substr(s, i+6); j=index(s2, "\""); name=substr(s2, 1, j-1) } \
			i=index(s, "classname=\""); \
			if (i > 0) { s2=substr(s, i+11); j=index(s2, "\""); cls=substr(s2, 1, j-1) } \
			selfclose = (s ~ /\/>/) \
		} \
		/<failure/   { status="FAIL" } \
		/<error/     { status="ERROR" } \
		/<skipped/   { status="SKIP" } \
		/<\/testcase>/ || (selfclose && /<testcase /) { \
			if (selfclose && /<testcase /) { pending=1 } else { pending=0 } \
		} \
		/<\/testcase>/ || selfclose { \
			if (status == "PASS")  { pass++;  printf "  \033[32m✓ PASS\033[0m   %s > %s\n", cls, name } \
			if (status == "FAIL")  { fail++;  printf "  \033[31m✗ FAIL\033[0m   %s > %s\n", cls, name } \
			if (status == "ERROR") { err++;   printf "  \033[31m! ERROR\033[0m  %s > %s\n", cls, name } \
			if (status == "SKIP")  { skip++;  printf "  \033[33m- SKIP\033[0m   %s > %s\n", cls, name } \
			selfclose = 0 \
		} \
		END { \
			printf "\n----------------------------------------\n"; \
			printf "  Total: %d  |  \033[32mPassed: %d\033[0m", total, pass; \
			if (fail > 0) printf "  |  \033[31mFailed: %d\033[0m", fail; \
			if (err > 0)  printf "  |  \033[31mErrors: %d\033[0m", err; \
			if (skip > 0) printf "  |  \033[33mSkipped: %d\033[0m", skip; \
			printf "\n========================================\n"; \
		}' target/surefire-reports/*.xml
	@echo ""
	@echo "  Full HTML report: $(REPORT)"
	@echo ""
