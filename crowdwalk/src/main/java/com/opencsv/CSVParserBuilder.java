/*
 Copyright 2005 Bytecode Pty Ltd.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.opencsv;


import com.opencsv.enums.CSVReaderNullFieldIndicator;

/**
 * Builder for creating a CSVParser.
 *<p>Example code for using this class:<br><br>
 * <code>
 * final CSVParser parser =<br>
 * new CSVParserBuilder()<br>
 * .withSeparator('\t')<br>
 * .withIgnoreQuotations(true)<br>
 * .build();<br>
 * </code></p>
 *
 * @see CSVParser
 */
public class CSVParserBuilder {

    private char separator = CSVParser.DEFAULT_SEPARATOR;
    private char quoteChar = CSVParser.DEFAULT_QUOTE_CHARACTER;
    private char escapeChar = CSVParser.DEFAULT_ESCAPE_CHARACTER;
    private boolean strictQuotes = CSVParser.DEFAULT_STRICT_QUOTES;
    private boolean ignoreLeadingWhiteSpace = CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE;
    private boolean ignoreQuotations = CSVParser.DEFAULT_IGNORE_QUOTATIONS;
    private CSVReaderNullFieldIndicator nullFieldIndicator = CSVReaderNullFieldIndicator.NEITHER;

    /**
     * Default constructor.
     */
    public CSVParserBuilder() {
    }

    /**
     * Sets the delimiter to use for separating entries.
     *
     * @param separator The delimiter to use for separating entries
     * @return The CSVParserBuilder
     */
    public CSVParserBuilder withSeparator(
            final char separator) {
        this.separator = separator;
        return this;
    }


    /**
     * Sets the character to use for quoted elements.
     *
     * @param quoteChar The character to use for quoted element.
     * @return The CSVParserBuilder
     */
    public CSVParserBuilder withQuoteChar(
            final char quoteChar) {
        this.quoteChar = quoteChar;
        return this;
    }


    /**
     * Sets the character to use for escaping a separator or quote.
     *
     * @param escapeChar The character to use for escaping a separator or quote.
     * @return The CSVParserBuilder
     */
    public CSVParserBuilder withEscapeChar(
            final char escapeChar) {
        this.escapeChar = escapeChar;
        return this;
    }


    /**
     * Sets the strict quotes setting - if true, characters
     * outside the quotes are ignored.
     *
     * @param strictQuotes If true, characters outside the quotes are ignored
     * @return The CSVParserBuilder
     */
    public CSVParserBuilder withStrictQuotes(
            final boolean strictQuotes) {
        this.strictQuotes = strictQuotes;
        return this;
    }

    /**
     * Sets the ignore leading whitespace setting - if true, white space
     * in front of a quote in a field is ignored.
     *
     * @param ignoreLeadingWhiteSpace If true, white space in front of a quote in a field is ignored
     * @return The CSVParserBuilder
     */
    public CSVParserBuilder withIgnoreLeadingWhiteSpace(
            final boolean ignoreLeadingWhiteSpace) {
        this.ignoreLeadingWhiteSpace = ignoreLeadingWhiteSpace;
        return this;
    }

    /**
     * Sets the ignore quotations mode - if true, quotations are ignored.
     *
     * @param ignoreQuotations If true, quotations are ignored
     * @return The CSVParserBuilder
     */
    public CSVParserBuilder withIgnoreQuotations(
            final boolean ignoreQuotations) {
        this.ignoreQuotations = ignoreQuotations;
        return this;
    }

    /**
     * Constructs CSVParser.
     * @return A new CSVParser with defined settings.
     */
    public CSVParser build() {
        return new CSVParser(
                separator,
                quoteChar,
                escapeChar,
                strictQuotes,
                ignoreLeadingWhiteSpace,
                ignoreQuotations,
                nullFieldIndicator);
    }

    /**
     * @return The defined separator.
     */
    public char getSeparator() {
        return separator;
    }

    /**
     * @return The defined quotation character.
     */
    public char getQuoteChar() {
        return quoteChar;
    }

    /**
     * @return The defined escape character.
     */
    public char getEscapeChar() {
        return escapeChar;
    }

    /**
     * @return The defined strict quotation setting.
     */
    public boolean isStrictQuotes() {
        return strictQuotes;
    }

    /**
     * @return The defined ignoreLeadingWhiteSpace setting.
     */
    public boolean isIgnoreLeadingWhiteSpace() {
        return ignoreLeadingWhiteSpace;
    }

    /**
     * @return The defined ignoreQuotation setting.
     */
    public boolean isIgnoreQuotations() {
        return ignoreQuotations;
    }

    /**
     * Sets the NullFieldIndicator.
     *
     * @param fieldIndicator CSVReaderNullFieldIndicator set to what should be considered a null field.
     * @return The CSVParserBuilder
     */
    public CSVParserBuilder withFieldAsNull(final CSVReaderNullFieldIndicator fieldIndicator) {
        this.nullFieldIndicator = fieldIndicator;
        return this;
    }

    /**
     * @return The null field indicator.
     */
    public CSVReaderNullFieldIndicator nullFieldIndicator() {
        return nullFieldIndicator;
    }
}
