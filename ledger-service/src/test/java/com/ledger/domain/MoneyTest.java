package com.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.domain.exception.DomainException;
import com.ledger.domain.money.Currency;
import com.ledger.domain.money.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void convertsExactDecimalToMinorUnits() {
        assertThat(Money.of(new BigDecimal("10.50"), Currency.USD).minorUnits()).isEqualTo(1050L);
        assertThat(Money.of(new BigDecimal("100"), Currency.JPY).minorUnits()).isEqualTo(100L);
    }

    @Test
    void rejectsAmountExceedingCurrencyPrecision() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("1.005"), Currency.USD))
                .isInstanceOf(DomainException.InvalidMoney.class);
        assertThatThrownBy(() -> Money.of(new BigDecimal("1.5"), Currency.JPY))
                .isInstanceOf(DomainException.InvalidMoney.class);
    }

    @Test
    void roundTripsThroughBigDecimal() {
        Money money = Money.of(new BigDecimal("42.42"), Currency.EUR);
        assertThat(money.toBigDecimal()).isEqualByComparingTo("42.42");
    }

    @Test
    void arithmeticRequiresMatchingCurrency() {
        Money usd = Money.ofMinor(100, Currency.USD);
        Money eur = Money.ofMinor(100, Currency.EUR);
        assertThatThrownBy(() -> usd.plus(eur)).isInstanceOf(DomainException.CurrencyMismatch.class);
    }

    @Test
    void negateAndSignsBehave() {
        Money money = Money.ofMinor(250, Currency.USD);
        assertThat(money.negate().minorUnits()).isEqualTo(-250L);
        assertThat(money.isPositive()).isTrue();
        assertThat(money.negate().isNegative()).isTrue();
        assertThat(Money.zero(Currency.USD).isZero()).isTrue();
    }

    @Test
    void rejectsNonPositivePostingAmount() {
        assertThatThrownBy(() -> Money.zero(Currency.USD).requirePositive())
                .isInstanceOf(DomainException.InvalidMoney.class);
    }
}
