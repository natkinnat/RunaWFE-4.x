/*
 * This file is part of the RUNA WFE project.
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation; version 2.1 
 * of the License. 
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU Lesser General Public License for more details. 
 * 
 * You should have received a copy of the GNU Lesser General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package ru.runa.af.web.action;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import ru.runa.common.web.Commons;
import ru.runa.common.web.ProfileHttpSessionHelper;
import ru.runa.common.web.Resources;
import ru.runa.common.web.TabHttpSessionHelper;
import ru.runa.common.web.action.ActionBase;
import ru.runa.wfe.security.AuthenticationException;
import ru.runa.wfe.security.auth.KerberosLoginModuleResources;
import ru.runa.wfe.security.auth.SubjectPrincipalsHelper;
import ru.runa.wfe.service.delegate.Delegates;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.user.User;

/**
 * This class provides Kerberos auth for IE
 * 
 * @struts:action path="/krblogin"
 * @struts.action-forward name="success" path="/manage_tasks.do" redirect =
 *                        "true"
 * @struts.action-forward name="failure" path="/start.do" redirect = "true"
 */
public class KrbLoginAction extends ActionBase {

    /* this must be changed if "success" forward changed! */
    private final static String DEFAULT_TAB_FORWARD_NAME = "manage_tasks";

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        try {
            if (!KerberosLoginModuleResources.isHttpAuthEnabled()) {
                throw new AuthenticationException("HTTP kerberos support disabled");
            }
            if (request.getUserPrincipal() == null) {
                throw new LoginException("No client name was provided.");
            }
            String domainActorName = request.getUserPrincipal().getName();
            int atIndex = domainActorName.indexOf("@");
            if (atIndex == -1) {
                throw new LoginException("Invalid domain name '" + domainActorName + "'");
            }
            String actorName = domainActorName.substring(0, atIndex);
            Actor actor = Delegates.getExecutorService().getActorCaseInsensitive(actorName);
            User user = SubjectPrincipalsHelper.createUser(actor);
            Delegates.getSystemService().login(user);
            Commons.setUser(user, request.getSession());
            ProfileHttpSessionHelper.reloadProfile(request.getSession());
            TabHttpSessionHelper.setTabForwardName(DEFAULT_TAB_FORWARD_NAME, request.getSession());
            saveToken(request);
        } catch (Exception e) {
            addError(request, e);
            return mapping.findForward(Resources.FORWARD_FAILURE);
        }
        return mapping.findForward(Resources.FORWARD_SUCCESS);
    }
}
