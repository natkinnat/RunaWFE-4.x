<table>
	<#list variables as variable>
	<tr>
		<td class="label">${variable.name}</td>
		<td class="input">${variable.tag}</td>
	</tr>
	</#list>
</table>