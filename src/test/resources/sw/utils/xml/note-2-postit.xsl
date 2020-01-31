<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<!--  XML output; Output file's SYSTEM = "postit.dtd" -->
	<xsl:output method="xml" omit-xml-declaration="no" doctype-system="postit.dtd" indent="yes" />
	
	<!-- Transform the 'PI' processing instruction -->
	<xsl:template match="processing-instruction()">
		<xsl:processing-instruction name="PI">Transformed processing instruction</xsl:processing-instruction>
	</xsl:template>
	
	<!--  Convert the 'note' element into 'postit' -->
	<xsl:template match="note">
		<!-- Create 'postit' element -->
		<xsl:element name="postit">
			<!--  Copy 'note.id' attribute to 'postit.postit-id' -->
			<xsl:attribute name="postit-id">
				<xsl:value-of select="@id" />
			</xsl:attribute>
			<!--  Apply templates for 'note.*' elements -->
			<xsl:apply-templates />
		</xsl:element>
	</xsl:template>
	
	<!--  Copy 'note.from' element to 'postit.from' -->
	<xsl:template match="from">
		<xsl:copy-of select="." />
	</xsl:template>

	<!-- Exclude 'note.to' and 'note.heading' elements -->
	<xsl:template match="to|heading" />
	
	<!--  Convert the 'note.body' element into 'postit.message' -->
	<xsl:template match="body">
		<xsl:element name="message"><xsl:value-of select="."/></xsl:element>
	</xsl:template>
</xsl:stylesheet>